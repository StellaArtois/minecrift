#!/usr/bin/env python

import os, os.path, sys
import shutil, glob, fnmatch
import subprocess, logging, shlex
from optparse import OptionParser

base_dir = os.path.dirname(os.path.abspath(__file__))

def cmdsplit(args):
    if os.sep == '\\':
        args = args.replace('\\', '\\\\')
    return shlex.split(args)

def create_patch( target_dir, src_file, mod_file, patch_file ):
    
    if os.name == 'nt':
        diff = os.path.abspath(os.path.join(base_dir, 'bin', 'diff.exe'))
        cmd = cmdsplit('"%s" -u "%s" "%s"' % (diff, src_file, mod_file ))
    else:
        cmd = cmdsplit('diff -u "%s" "%s"' % ( src_file, mod_file ) )

    process = subprocess.Popen(cmd, cwd=target_dir, bufsize=-1, stdout=subprocess.PIPE)
    stdout, stderr = process.communicate()
    with open( patch_file, 'w') as out:
        out.write(stdout)

def main(mcp_dir):
    sys.path.append(mcp_dir)
    os.chdir(mcp_dir)
    from runtime.getchangedsrc import getchangedsrc
    getchangedsrc( None, False, False )
    os.chdir( base_dir )

    new_src_dir = os.path.join( base_dir , "src" )
    patch_dir = os.path.join( base_dir , "patches" )

    mod_src_dir = os.path.join( mcp_dir , "modsrc", "minecraft" )
    org_src_dir = os.path.join( mcp_dir , "src", ".minecraft_orig" )

    for src_dir, dirs, files in os.walk(mod_src_dir):
        new_dir = src_dir.replace(mod_src_dir, new_src_dir)
        mod_dir = src_dir.replace(mod_src_dir, org_src_dir)
        if not os.path.exists(new_dir):
            os.mkdir(new_dir)
        for file_ in files:
            src_file = os.path.join(src_dir, file_)
            new_file = os.path.join(new_dir, file_)
            org_file = os.path.join(mod_dir, file_)
            if os.path.exists(org_file):
                patch_file_dir = src_dir.replace( mod_src_dir, patch_dir)
                try:
                    os.makedirs( patch_file_dir )
                except os.error as e:
                    pass
                patch_file = os.path.join(patch_file_dir,file_+".patch")

                mod_file = src_file.replace( mcp_dir, "." )
                org_file = org_file.replace( mcp_dir, "." )

                create_patch( mcp_dir, org_file, mod_file, patch_file )
            else:
                #new class file, just replace
                if os.path.exists( new_file ):
                    os.remove( new_file )
                shutil.copy(src_file, new_dir)

    
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
