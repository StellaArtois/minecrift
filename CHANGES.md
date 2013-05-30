0.26 Alpha 29-05-13
===================
- First open source release.
- Added plugin system for alternative implementations choosable at runtime.
- Added depth dependent crosshair.
- Refined neck model.
- Moved camera backwards to prevent 
- Moved first-person model backwards even further than camera.
- Fixed bed camera control.
- Fixed UI transparency flickering.
- Made neck model configurable.

0.25 Alpha 22-05-13 [Currently no MAC dynlib built for this, sorry - incoming soon I hope]
===================
- Now take into account the mouse inversion setting when using mouse pitch input.
- Ctrl , or . decreases or increases the FOV scale factor. 
- Ctrl-Alt , or . decreases or increases the size of the black distortion
border. This can be used to improve rendering speed with large border sizes (at
a cost to FOV), or *potentially* give more peripheral vision at smaller border
sizes.
- Ctrl V cycles through head track sensitivity multipliers. Try this at your own risk!

0.24 Alpha 18-05-13
===================
- The HUD is now completely fixed to head orientation to make it slightly more readable.
- Implemented Full Scene Anti-aliasing support (FSAA). Be WARNED! This is a
resource hog! Use Ctrl-B to toggle on/off. Use Ctrl-Alt B to cycle through FSAA
scale factors.

0.23 Alpha 15-05-13
===================
- Initial MacOS support, with thanks to @PyramidHead76!
- Support chromatic aberration correction (beta)(with thanks to Ben @ MTBS3D). Toggle via Ctrl-Alt P.
- HUD GUI menu background now transparent.
- Potential fix for 'lost mouse' issue in HUD when not fullscreen or when using multiple monitors.
- Allow user to specify HUD scale. Use Ctrl-Alt U to cycle values.
- Allow user to specify HUD distance. Use Ctrl U to cycle values.
- Allow user to use mouse pitch input as well as yaw. Use Ctrl-N to toggle.

0.22 Alpha 12-05-13
===================
- Reinstated crosshair when HUD overlay is on.
- Corrected aspect ratio of HUD.
- Fixed rendering of health, experience bars etc.

0.21 Alpha 10-05-13
===================
- Potential fix for other players' heads not rendering.
- Initial support for hiding the player's head wear / masks. Use Ctrl-M to toggle the mask rendering.

0.20 Alpha 10-05-13
===================
- Fix 'late night' build issues with 0.19:
   - Fixed unsatisified link error for head track prediction.
   - Removed test code(!); for users without a Rift plugged in the screen kept rolling from side to side!

0.19 Alpha 09-05-13
===================
- Reverted to fixed position HUD.
- Large GUI size recommended.
- Fixed mouse pointer alignment issue on menus.
- Possible fix for crash on exit.
- Added head track prediction support. Ctrl-Alt L to toggle.

0.18 Alpha 08-05-13
===================
- Attempt to fix crash while head track enabled in menus.
- Reduced opacity of HUD.

0.17 Alpha 08-05-13
===================
- Prototype 3D GUI.

0.16 Alpha 07-05-13
===================
- Yet more head tracking changes.

0.15 Alpha 07-05-13
===================
- Further adjustments to head tracking.
- Additional head-track position data output to debug display.
- Lock HUD position toggle via Ctrl-U added.

0.14 Alpha 06-05-13
===================
- Further head tracking fix to convert Euler angles from the rift to degrees for Minecraft.

0.13 Alpha 06-05-13
===================
- Potential fix to continuously accelerating yaw rotation using head tracking.

0.12 Alpha 06-05-13
===================
- Ctrl-L to toggle headtracking on / off, allowing mouse look.

0.11 Alpha - FIRST RELEASE 06-05-13
===================================
- For Minecraft 1.5.2
- Initial Rift stereo rendering and distortion in place, with IPD adjustment available.
- Headtracking working (maybe! I have no means of testing this currently!).
- Proper first-person rendering.
- Rudimentary neck model should be in place.
- Removed crosshair.
- Initial overlay HUD.
