import os, os.path, sys
import shutil, fnmatch
import subprocess, shlex
from optparse import OptionParser

base_dir = os.path.dirname(os.path.abspath(__file__))

#Helpers taken from forge mod loader, https://github.com/MinecraftForge/FML/blob/master/install/fml.py
def cmdsplit(args):
    if os.sep == '\\':
        args = args.replace('\\', '\\\\')
    return shlex.split(args)

def apply_patch( mcp_dir, patch_file, target_dir ):
    if os.name == 'nt':
        applydiff = os.path.abspath(os.path.join(mcp_dir, 'runtime', 'bin', 'applydiff.exe'))
        cmd = cmdsplit('"%s" -uf -p1 -i "%s"' % (applydiff, patch_file ))
    else:
        cmd = cmdsplit('patch -p1 -i "%s" ' % patch_file )

    process = subprocess.Popen(cmd, cwd=target_dir, bufsize=-1)
    process.communicate()

def apply_patches(mcp_dir, patch_dir, target_dir, find=None, rep=None):
    for path, _, filelist in os.walk(patch_dir, followlinks=True):
        for cur_file in fnmatch.filter(filelist, '*.patch'):
            patch_file = os.path.normpath(os.path.join(patch_dir, path[len(patch_dir)+1:], cur_file))
            apply_patch( mcp_dir, patch_file, target_dir )

def merge_tree(root_src_dir, root_dst_dir):
    for src_dir, dirs, files in os.walk(root_src_dir):
        dst_dir = src_dir.replace(root_src_dir, root_dst_dir)
        if not os.path.exists(dst_dir):
            os.mkdir(dst_dir)
        for file_ in files:
            src_file = os.path.join(src_dir, file_)
            dst_file = os.path.join(dst_dir, file_)
            print("Copying file %s" % src_file.replace(root_src_dir+"/","") )
            if os.path.exists(dst_file):
                os.remove(dst_file)
            shutil.copy(src_file, dst_dir)

def main(mcp_dir):
    print("Applying Changes...")

    src_dir = os.path.join(mcp_dir, "src","minecraft")
    mod_bak_dir = os.path.join(mcp_dir, "src","minecraft-bak")
    bak_dir = os.path.join(mcp_dir, "src",".minecraft_orig")
    
    print("Backing up src/minecraft to src/minecraft-bak")
    shutil.rmtree( mod_bak_dir, True )
    shutil.move( src_dir, mod_bak_dir )
    shutil.copytree( bak_dir, src_dir )

    #apply patches
    apply_patches( mcp_dir, os.path.join( base_dir, "patches"), src_dir )
    #merge in the new classes
    merge_tree( os.path.join( base_dir, "src" ), src_dir )
    
if __name__ == '__main__':
    parser = OptionParser()
    parser.add_option('-m', '--mcp-dir', action='store', dest='mcp_dir', help='Path to MCP to use', default=None)
    options, _ = parser.parse_args()

    if not options.mcp_dir is None:
        main(os.path.abspath(options.mcp_dir))
    elif os.path.isfile(os.path.join('..', 'runtime', 'commands.py')):
        main(os.path.abspath('..'))
    else:
        main(os.path.abspath('mcp'))
