MineCrift
=========

A mod for minecraft enabling the Virtual Reality HMD, Oculus Rift.

To build:

The installation process has been tested on Linux, but was written with
cross-platform support in mind, so should be usable on other platforms.
Known issues: OSX fernflower doesn't decompile cleanly. Decompile on Windows
or Linux and copy over the .minecraft\_orig folder.

Download [mcp 751](http://mcp.ocean-labs.de/index.php/MCP_Releases) and
extract into /mcp (only needed once)

Run install.sh (or install.bat) to download minecraft, download optifine,
deobfuscate the base system, and apply the patches and new files.


Use the MCP environment in /mcp to modify, test, and recompile.

Run build.sh (or build.bat) to create a release .zip. (not currently
implemented)

Run getchanges.sh (or getchanges.bat) to diff the modified /mcp/src files into
version controlled /patches and copy the new classes into the /src/
directory.

