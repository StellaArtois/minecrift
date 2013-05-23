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

def create_patch( target_dir, src_file, mod_file, label, patch_file ):
    
    if os.name == 'nt':
        diff = os.path.abspath(os.path.join(base_dir, 'bin', 'diff.exe'))
    else:
        diff = "diff"

    cmd = cmdsplit('"%s" -u --label "a/%s" "%s" --label "b/%s" "%s"' % (diff, label, src_file, label, mod_file ))

    process = subprocess.Popen(cmd, cwd=target_dir, bufsize=-1, stdout=subprocess.PIPE)
    stdout, stderr = process.communicate()
    if stdout:
        with open( patch_file, 'w') as out:
            out.write(stdout)

def main(mcp_dir):
    new_src_dir    = os.path.join( base_dir , "src" )
    patch_base_dir = os.path.join( base_dir , "patches" )

    mod_src_dir = os.path.join( mcp_dir , "src", "minecraft" )
    org_src_dir = os.path.join( mcp_dir , "src", ".minecraft_orig" )

    for src_dir, dirs, files in os.walk(mod_src_dir):
        pkg       = os.path.relpath(src_dir,mod_src_dir)
        new_dir   = os.path.join( new_src_dir,    pkg )
        mod_dir   = os.path.join( org_src_dir,    pkg )
        patch_dir = os.path.join( patch_base_dir, pkg )
        if not os.path.exists(new_dir):
            os.mkdir(new_dir)
        if not os.path.exists(patch_dir):
            os.mkdir(patch_dir)
        for file_ in files:
            mod_file = os.path.join(src_dir, file_)
            org_file = os.path.join(mod_dir, file_)

            if os.path.exists(org_file):
                patch_file = os.path.join(patch_dir,file_+".patch")
                label = pkg.replace("\\","/") + "/" + file_ #patch label always has "/"

                create_patch( mcp_dir, org_file, mod_file, label, patch_file )
            else:
                new_file = os.path.join(new_dir, file_)
                #new class file, just replace
                if os.path.exists( new_file ):
                    os.remove( new_file )
                shutil.copy(mod_file, new_dir)

    
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
