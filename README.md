Minecrift Mod for Minecraft
===========================

Current Version: 0.26 alpha

StellaArtois, mabrowning 2013


With thanks to:

- Palmer Luckey and his team for creating the Oculus Rift. The future is
  finally here (well for some people anyway; mine hasn't arrived yet).
- Markus "Notch" Persson for creating Minecraft. What has it grown into?
- The team behind the MCP coders' pack, and the Minecraft community - why
  Mojang bother obsfucating the source when you guys have done such a fantastic
  job of de-obsfucating it is beyond me!
- Powback for his initial work on the Java JNI wrapper to the SDK. Seeing this
  inspired me to get off my arse and get modding. See
  [this Reddit thread](http://www.reddit.com/r/oculus/comments/1c1vh0/java_wrapper_for_devs/)
- shakesoda and Ben (and others?) at MTBS for creating the GLSL version of the
  oculus distortion shader.
- The guys at Valve for giving some good advice on updating a game for VR.
- @PyramidHead76 for building the MacOS libs, and toiling to produce the
  installation guide!!
- Brad Larson and his GPUImage library, for the Lanczos GLSL shader
  implementation for the FSAA.
- mabrowning for his amazing on-going work to get Optifine and Minecrift
  integrated. Soon it'll be his name as author, with me as a small credit...

No thanks to:

- My lack of OpenGL experience.

What is Minecrift?
------------------

The cheesy name apart, Minecrift attempts to update Minecraft to support the
Oculus Rift. Initially this means allowing headtracking input and using the
correct stereo rendering parameters for the Rift. In the future this also means
updating Minecraft for various control schemes, and updating the GUI to be in
full 3D. Minecrift is also meant as a kick up the arse to Mojang, so that they
can add official Oculus support in the near future. As and when Minecraft
officially supports the Rift, Minecrift development will probably cease (unless
they make a complete hash of it).


Disclaimer
----------

I recommend using a vanila Minecraft.jar file for this. Forge compatibility is
patially in place, but not copmleted.  BACK UP your original minecraft.jar
before installing this mod. I make no claims as to the compatibility of this
mod with other mods other than Optifine!

Installation
------------

REQUIRES Minecraft 1.5.2 With Optifine HD D2 or D3. 

Windows
-------

Minecrift for Windows requires Vista or above and a graphics card & driver capable of at least OpenGL 3.3 support.

- Change directory to %APPDATA%\.minecraft\bin
- Open your minecraft.jar file using 7-zip, winzip etc. 
- Select all, and drag and drop in the entire contents of the /minecraft
  directory from the Minecrift zip into the jar archive (but not the /minecraft
  directory itself).
- Make sure to delete the META-INF folder in minecraft.jar. Close 7zip /
  winzip.
- Copy JRiftLibrary.dll and JRiftLibrary64.dll into <Path to
  %APPDATA%>\.minecraft\bin\natives
- *VERY IMPORTANT* Go to the My Computer icon, right click, select properties.
  Go to advanced system settings, Environment variables. Edit the *system* path
  to add the directory <Path to %APPDATA%>\\.minecraft\bin\natives, so that the
  JRiftLibrary dlls can be found. If you don't do this, Minecraft will just
  show a black screen on startup!
- *AS IMPORTANT*. Install the Microsoft VS2012 C++ redists (both x86
  and x64) from
  [here](http://www.microsoft.com/visualstudio/11/en-us/downloads/vc-redist#vc-redist)
- Run up Minecraft and off you go. If you get a black screen on login, trying
  running an admin command prompt, cd to your minecraft.exe dir and enter the
  command 
>java -cp Minecraft.exe net.minecraft.LauncherFrame
This should allow any exceptions or errors on Minecraft startup to show up in the console.

MacOS
-----


Controls
--------

Once in game the dual viewports will kick in. I suggest using Large or Normal
GUI size for now. Obviously you will want to be at 1280X800 fullscreen for the
Oculus currently. 


- F1 to bring up the game HUD / overlay if it isn't already up. 
- Cycle F3 until the Rift debug info appears. 
- Ctrl and - / = for IPD adjustment. Hold ALT as well for fine adjustment. The
  IPD setting should be saved between sessions.
- Ctrl O to attempt to reinitialise the Rift (including head tracking).
- Ctrl P while not in a menu to turn distortion on / off. Sometimes useful if
  the offset mouse pointer is a pain in the menus. Ctrl-Alt P to toggle
  chromatic aberration correction.
- Ctrl L toggles headtracking ON/OFF. Ctrl-Alt L toggles tracking prediction
  ON/OFF. It is OFF by default.
- Ctrl U changes the HUD distance. Ctrl-Alt U changes the HUD scale. Ctrl-Alt Y
  toggles opacity on the HUD.
- Ctrl-M toggles rendering of the player's mask ON/OFF.
- FOV adjustment within Minecraft will have no effect - I use the FOV as
  calculated from within the Oculus SDK.
- Allow user to use mouse pitch input as well as yaw. Use Ctrl-N to toggle.
- Large or Auto GUI size recommended.
- Use Ctrl-B to turn Full Scene Anti-aliasing (FSAA) on/off. Use Ctrl-Alt B to
  cycle the FSAA renderscale. Be warned; this feature is a resource hog!! If
  you cannot get 60fps at your desired FSSA level, cycle it to a lower scale
  factor. Anyone with a nVidia GTX Titan please let me know what average FPS
  you get at scale factor 4.0!
- Ctrl , or . decreases or increases the FOV scale factor. This can be used to
  fine tune FOV if it doesn't look quite right to you.
- Ctrl-Alt , or . decreases or increases various sizes of distortion border.
  This can be used to improve rendering speed, at a potential loss of FOV.
- Ctrl V cycles through head track sensitivity multipliers. Try this at your
  own risk!

Known Issues
------------

There are (not so) many.

- The GUI is on it's way to being 3D. It's still going to be a little rough
  around the edges however, and is in prototype stage.
- The crosshair isn't exactly pair to the eyes and can move slightly off the
  center of the screen when close to objects. It is still accurate, however, so
  trust the crosshairs no matter where they are: that is where you will click.


Feedback, bug reporting
-----------------------

Please post feedback, bug reports etc. to the forum thread at MTBS: http://www.mtbs3d.com/phpbb/viewtopic.php?f=140&t=17146

Roadmap
-------

- Get 3D GUI working well.
- Add all current keyboard commands to the options menu GUI.
- Alternate control scheme options; head look separate to body movement etc.
- Investigate gamepad / Razor Hydra support.
- Fix bugs.

Release Notes
=============

Known Issues:
-------------

- A white line can sometimes be seen at the top or bottom edge of the HUD. Scaling the HUD down one point should remove it.
- Sometimes an in-game menu will not appear until another menu has been opened and closed first.
- A settings menu GUI is needed for all the Minecrift settings!


Building
========



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

License
-------

See [The License File](LICENSE.md) for more information.
