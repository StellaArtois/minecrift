package com.mtbs3d.minecrift;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import de.fruitfly.ovr.EyeRenderParams;
import de.fruitfly.ovr.HMDInfo;
import de.fruitfly.ovr.IOculusRift;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;
import net.minecraft.src.*;

import static java.lang.Math.ceil;

public class VRRenderer extends EntityRenderer
{
    public float totalMouseYawDelta = 0.0f;
    public float totalMousePitchDelta = 0.0f;


    public boolean _FBOInitialised = false;
    int _shaderProgramId = -1;
    int _frameBufferId = -1;
    int _colorTextureId = -1;
    int _depthRenderBufferId = -1;

    int _GUIshaderProgramId = -1;
    int _GUIframeBufferId = -1;
    int _GUIcolorTextureId = -1;
    int _GUIdepthRenderBufferId = -1;
    int _GUIscaledWidth = 0;
    int _GUIscaledHeight = 0;
    float _GUIscaleFactor = 0.0f;

    int _Lanczos_shaderProgramId = -1;
    int _Lanczos_GUIframeBufferId1 = -1;
    int _Lanczos_GUIcolorTextureId1 = -1;
    int _Lanczos_GUIdepthRenderBufferId1 = -1;
    int _Lanczos_GUIframeBufferId2 = -1;
    int _Lanczos_GUIcolorTextureId2 = -1;
    int _Lanczos_GUIdepthRenderBufferId2 = -1;

    // VBO stuff
    // Quad variables
    private int vaoId = 0;
    private int vboId = 0;
    private int vboiId = 0;
    private int indicesCount = 0;

    int _position_id = -1;
    int _inputTextureCoordinate_id = -1;

    int _previousDisplayWidth = 0;
    int _previousDisplayHeight = 0;
    int lastMouseMaxOffsetX = 0;
    int lastMouseMaxOffsetY = 0;
    public int mouseX = 0;
    public int mouseY = 0;
    IOculusRift oculusRift;
    GuiAchievement guiAchievement;
    EyeRenderParams eyeRenderParams;
	private double renderViewEntityX;
	private double renderViewEntityY;
	private double renderViewEntityZ;
    
    boolean superSampleSupported;
	private float pointX;
	private float pointZ;
	private float pointY;

    public VRRenderer(Minecraft par1Minecraft, IOculusRift rift, GuiAchievement guiAchiv )
    {
    	super( par1Minecraft );
    	this.oculusRift = rift;
    	this.guiAchievement = guiAchiv;
    	
    	try
    	{
    		GL30.glBindVertexArray(0);
    		superSampleSupported = true;
    		
    	}
    	catch( IllegalStateException e )
    	{
    		superSampleSupported = false;
    		
    	}
    }

    /**
     * sets up projection, view effects, camera position/rotation
     */
    private void setupCameraTransform(float renderPartialTicks, int renderSceneNumber)
    {
        this.farPlaneDistance = (float)this.mc.gameSettings.ofRenderDistanceFine;

        if (Config.isFogFancy())
        {
            this.farPlaneDistance *= 0.95F;
        }

        if (Config.isFogFast())
        {
            this.farPlaneDistance *= 0.83F;
        }

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        if (renderSceneNumber == 0)
        {
            // Left eye
            FloatBuffer leftProj = eyeRenderParams.gl_getLeftProjectionMatrix();
            GL11.glLoadMatrix(leftProj);
            mc.checkGLError("Set left projection");
        }
        else
        {
            // Right eye
            FloatBuffer rightProj = eyeRenderParams.gl_getRightProjectionMatrix();
            GL11.glLoadMatrix(rightProj);
            mc.checkGLError("Set right projection");
        }
        float var5;

        if (this.mc.playerController.enableEverythingIsScrewedUpMode())
        {
            var5 = 0.6666667F;
            GL11.glScalef(1.0F, var5, 1.0F);
        }

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        //First, IPD transformation (and possible toe-in)
        if (renderSceneNumber == 0)
        {
            // Left eye
            FloatBuffer leftEyeTransform = eyeRenderParams.gl_getLeftViewportTransform();
            GL11.glMultMatrix(leftEyeTransform);
        }
        else
        {
            // Right eye
            FloatBuffer rightEyeTransform = eyeRenderParams.gl_getRightViewportTransform();
            GL11.glMultMatrix(rightEyeTransform);
        }

        //Next, possible neck-model transformation
        EntityLiving entity = this.mc.renderViewEntity;
	    float cameraYOffset = entity.yOffset - (this.mc.gameSettings.playerHeight - this.mc.gameSettings.neckBaseToEyeHeight);
	    GL11.glTranslatef(0.0f, -this.mc.gameSettings.neckBaseToEyeHeight, this.mc.gameSettings.eyeProtrusion);


        //A few game effects
        this.hurtCameraEffect(renderPartialTicks);

        if (this.mc.gameSettings.viewBobbing)
        {
            this.setupViewBobbing(renderPartialTicks);
        }
        
        double camX = renderViewEntityX;
        double camY = renderViewEntityY - cameraYOffset;
        double camZ = renderViewEntityZ;
      
        var5 = this.mc.thePlayer.prevTimeInPortal + (this.mc.thePlayer.timeInPortal - this.mc.thePlayer.prevTimeInPortal) * renderPartialTicks;

        if (var5 > 0.0F)
        {
            byte var6 = 20;

            if (this.mc.thePlayer.isPotionActive(Potion.confusion))
            {
                var6 = 7;
            }

            float var7 = 5.0F / (var5 * var5 + 5.0F) - var5 * 0.04F;
            var7 *= var7;
            GL11.glRotatef(((float)this.rendererUpdateCount + renderPartialTicks) * (float)var6, 0.0F, 1.0F, 1.0F);
            GL11.glScalef(1.0F / var7, 1.0F, 1.0F);
            GL11.glRotatef(-((float)this.rendererUpdateCount + renderPartialTicks) * (float)var6, 0.0F, 1.0F, 1.0F);
        }

        if (entity.isPlayerSleeping())
        {
            
            if (!this.mc.gameSettings.debugCamEnable) 
            {
            	cameraYOffset = (float)((double)cameraYOffset + 1.0D);
                GL11.glTranslatef(0.0F, 0.3F, 0.0F);

                int var10 = this.mc.theWorld.getBlockId(MathHelper.floor_double(entity.posX), MathHelper.floor_double(entity.posY), MathHelper.floor_double(entity.posZ));

                if (Reflector.ForgeHooksClient_orientBedCamera.exists())
                {
                    Reflector.callVoid(Reflector.ForgeHooksClient_orientBedCamera, new Object[] {this.mc, entity});
                }
                else if (var10 == Block.bed.blockID)
                {
                    int var11 = this.mc.theWorld.getBlockMetadata(MathHelper.floor_double(entity.posX), MathHelper.floor_double(entity.posY), MathHelper.floor_double(entity.posZ));
                    int var12 = var11 & 3;
                    GL11.glRotatef((float)(var12 * 90), 0.0F, 1.0F, 0.0F);
                }

                GL11.glRotatef(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * renderPartialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
                GL11.glRotatef(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * renderPartialTicks, -1.0F, 0.0F, 0.0F);
            }
        }
        else if (this.mc.gameSettings.thirdPersonView > 0)
        {
            double thirdPersonCameraDist = (double)(this.thirdPersonDistanceTemp + (this.thirdPersonDistance - this.thirdPersonDistanceTemp) * renderPartialTicks);
            float thirdPersonYaw;
            float thirdPersonPitch;

            if (this.mc.gameSettings.debugCamEnable)
            {
                thirdPersonYaw = this.prevDebugCamYaw + (this.debugCamYaw - this.prevDebugCamYaw) * renderPartialTicks;
                thirdPersonPitch = this.prevDebugCamPitch + (this.debugCamPitch - this.prevDebugCamPitch) * renderPartialTicks;
                GL11.glTranslatef(0.0F, 0.0F, (float)(-thirdPersonCameraDist));
                GL11.glRotatef(thirdPersonYaw, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(thirdPersonPitch, 0.0F, 1.0F, 0.0F);
            }
            else
            {
                thirdPersonYaw = entity.rotationYaw;
                thirdPersonPitch = entity.rotationPitch;

                if (this.mc.gameSettings.thirdPersonView == 2)
                {
                    thirdPersonPitch += 180.0F;
                }

                double camXOffset = (double)(-MathHelper.sin(thirdPersonYaw / 180.0F * (float)Math.PI) * MathHelper.cos(thirdPersonPitch / 180.0F * (float)Math.PI)) * thirdPersonCameraDist;
                double camZOffset = (double)( MathHelper.cos(thirdPersonYaw / 180.0F * (float)Math.PI) * MathHelper.cos(thirdPersonPitch / 180.0F * (float)Math.PI)) * thirdPersonCameraDist;
                double camYOffset = (double)(-MathHelper.sin(thirdPersonPitch   / 180.0F * (float)Math.PI)) * thirdPersonCameraDist;
                
                //This loop offsets at [-.1, -.1, -.1], [.1,-.1,-.1], [.1,.1,-.1] etc... for all 8 directions
                for (int var20 = 0; var20 < 8; ++var20)
                {
                    float var21 = (float)((var20 & 1) * 2 - 1);
                    float var22 = (float)((var20 >> 1 & 1) * 2 - 1);
                    float var23 = (float)((var20 >> 2 & 1) * 2 - 1);
                    var21 *= 0.1F;
                    var22 *= 0.1F;
                    var23 *= 0.1F;
                    MovingObjectPosition var24 = this.mc.theWorld.rayTraceBlocks(
                    		this.mc.theWorld.getWorldVec3Pool().getVecFromPool(camX + (double)var21, camY + (double)var22, camZ + (double)var23), 
                    		this.mc.theWorld.getWorldVec3Pool().getVecFromPool(camX - camXOffset + (double)var21 + (double)var23, camY - camYOffset + (double)var22, camZ - camZOffset + (double)var23));

                    if (var24 != null)
                    {
                        double var25 = var24.hitVec.distanceTo(this.mc.theWorld.getWorldVec3Pool().getVecFromPool(camX, camY, camZ));

                        if (var25 < thirdPersonCameraDist)
                        {
                            thirdPersonCameraDist = var25;
                        }
                    }
                }

                if (this.mc.gameSettings.thirdPersonView == 2)
                {
                    GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
                }

                GL11.glRotatef(entity.rotationPitch - thirdPersonPitch, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(entity.rotationYaw - thirdPersonYaw, 0.0F, 1.0F, 0.0F);
                GL11.glTranslatef(0.0F, 0.0F, (float)(-thirdPersonCameraDist));
                GL11.glRotatef(thirdPersonYaw - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(thirdPersonPitch - entity.rotationPitch, 1.0F, 0.0F, 0.0F);
            }
        }
        

        if (!this.mc.gameSettings.debugCamEnable)
        {
            this.prevCamRoll = this.camRoll;
            GL11.glRotatef(this.camRoll, 0.0F, 0.0F, 1.0F);

            if (oculusRift.isInitialized())
            {
                // Use direct values
                GL11.glRotatef(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(entity.rotationYaw + 180.0F, 0.0F, 1.0F, 0.0F);
            }
            else
            {
                GL11.glRotatef(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * renderPartialTicks, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * renderPartialTicks + 180.0F, 0.0F, 1.0F, 0.0F);
            }
        }


        GL11.glTranslatef(0.0F, cameraYOffset, 0.0F);

        if (this.debugViewDirection > 0)
        {
            int var8 = this.debugViewDirection - 1;

            if (var8 == 1)
            {
                GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            }

            if (var8 == 2)
            {
                GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            }

            if (var8 == 3)
            {
                GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
            }

            if (var8 == 4)
            {
                GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            }

            if (var8 == 5)
            {
                GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            }
        }
    }
    
    protected void updateCamera( float renderPartialTicks, boolean displayActive )
    {
        if (oculusRift.isInitialized() && this.mc.gameSettings.useHeadTracking)
        {
            this.mc.mcProfiler.startSection("oculus");

            if (displayActive)
            {
                // Read head tracker orientation
                oculusRift.poll();

                if (this.mc.inGameHasFocus && displayActive)
                {
                    this.mc.mouseHelper.mouseXYChange();
                    float mouseSensitivityMultiplier1 = this.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
                    float mouseSensitivityMultiplier2 = mouseSensitivityMultiplier1 * mouseSensitivityMultiplier1 * mouseSensitivityMultiplier1 * 8.0F;
                    float adjustedMouseDeltaX = (float)this.mc.mouseHelper.deltaX * mouseSensitivityMultiplier2;
                    float adjustedMouseDeltaY = (float)this.mc.mouseHelper.deltaY * mouseSensitivityMultiplier2;
                    byte yDirection = -1;

                    if (this.mc.gameSettings.invertMouse)
                    {
                        yDirection = 1;
                    }

                    if (this.mc.gameSettings.smoothCamera)
                    {
                        this.smoothCamYaw += adjustedMouseDeltaX;
                        float smoothTicks = renderPartialTicks - this.smoothCamPartialTicks;
                        this.smoothCamPartialTicks = renderPartialTicks;
                        adjustedMouseDeltaX = this.smoothCamFilterX * smoothTicks;
                        adjustedMouseDeltaY = this.smoothCamFilterY * smoothTicks * (float)yDirection;
                    }

                    totalMouseYawDelta += adjustedMouseDeltaX;
                    totalMouseYawDelta %= 360;

                    // Allow mouse pitch delta
                    totalMousePitchDelta += (adjustedMouseDeltaY * (float)yDirection);
                    if (totalMousePitchDelta > 180.0f)
                        totalMousePitchDelta = 180.0f;
                    else if (totalMousePitchDelta < -180.0f)
                        totalMousePitchDelta = -180.f;
                }

                float oculusYaw   = (totalMouseYawDelta                                                  + (oculusRift.getYawDegrees_LH()   * this.mc.gameSettings.headTrackSensitivity)) % 360;
                float oculusPitch = (mc.gameSettings.allowMousePitchInput ? totalMousePitchDelta: 0.0f ) + (oculusRift.getPitchDegrees_LH() * this.mc.gameSettings.headTrackSensitivity) ;
                
                // Correct for gimbal lock prevention
                if (oculusPitch > IOculusRift.MAXPITCH)
                    oculusPitch = IOculusRift.MAXPITCH;
                else if (oculusPitch < -IOculusRift.MAXPITCH)
                    oculusPitch = -IOculusRift.MAXPITCH;
                                                             // Roll multiplier is a one-way trip to barf-ville!
                float roll = oculusRift.getRollDegrees_LH() * this.mc.gameSettings.headTrackSensitivity;

                if (roll > IOculusRift.MAXROLL)
                    roll = IOculusRift.MAXROLL;
                else if (roll < -IOculusRift.MAXROLL)
                    roll = -IOculusRift.MAXROLL;

                if (this.mc.thePlayer != null)
                {
                    this.mc.thePlayer.setAnglesAbsolute(oculusYaw, oculusPitch, roll);
                    this.camRoll = roll;
                    //System.out.println(String.format("Set angles: %.2f, %.2f, %.2f", new Object[] {Float.valueOf(oculusYaw), Float.valueOf(oculusRift.getPitch()), Float.valueOf(oculusRift.getRoll())}));
                }
            }
            this.mc.mcProfiler.endSection();
        }
        else
        {
            super.updateCamera(renderPartialTicks, displayActive);
        }
    }
    
    public void renderWorld(float renderPartialTicks, long nextFrameTime)
    {
        if (this.mc.renderViewEntity == null)
        {
            this.mc.renderViewEntity = this.mc.thePlayer;
        }

        this.mc.mcProfiler.endStartSection("pick");
        this.getMouseOver(renderPartialTicks);
        EntityLiving renderViewEntity = this.mc.renderViewEntity;
        RenderGlobal renderGlobal = this.mc.renderGlobal;
        EffectRenderer effectRenderer = this.mc.effectRenderer;
        this.mc.mcProfiler.endStartSection("center");

        if ( superSampleSupported && this.mc.gameSettings.useSupersample)
        {
            eyeRenderParams = oculusRift.getEyeRenderParams(0,
                    0,
                    (int)ceil(this.mc.displayWidth * this.mc.gameSettings.superSampleScaleFactor),
                    (int)ceil(this.mc.displayHeight * this.mc.gameSettings.superSampleScaleFactor),
                    0.05F,
                    this.farPlaneDistance * 2.0F,
                    this.mc.gameSettings.fovScaleFactor,
                    getDistortionFitX(),
                    getDistortionFitY());
        }
        else
        {
            eyeRenderParams = oculusRift.getEyeRenderParams(0,
                    0,
                    this.mc.displayWidth,
                    this.mc.displayHeight,
                    0.05F,
                    this.farPlaneDistance * 2.0F,
                    this.mc.gameSettings.fovScaleFactor,
                    getDistortionFitX(),
                    getDistortionFitY());
        }

        if (this.mc.displayWidth != _previousDisplayWidth || this.mc.displayHeight != _previousDisplayHeight || !_FBOInitialised)
        {
            _FBOInitialised = false;

            _previousDisplayWidth = this.mc.displayWidth;
            _previousDisplayHeight = this.mc.displayHeight;

            lastMouseMaxOffsetX = 0;
            lastMouseMaxOffsetY = 0;

            // TODO: Clean up old shader object if initialised

            // Main render FBO
            if (_depthRenderBufferId != -1)
            {
                GL30.glDeleteRenderbuffers(_depthRenderBufferId);
                _depthRenderBufferId = -1;
            }

            if (_colorTextureId != -1)
            {
                GL11.glDeleteTextures(_colorTextureId);
                _colorTextureId = -1;
            }

            if (_frameBufferId != -1)
            {
                GL30.glDeleteFramebuffers(_frameBufferId);
                _frameBufferId = -1;
            }

            // GUI FBO
            if (_GUIdepthRenderBufferId != -1)
            {
                GL30.glDeleteRenderbuffers(_GUIdepthRenderBufferId);
                _GUIdepthRenderBufferId = -1;
            }

            if (_GUIcolorTextureId != -1)
            {
                GL11.glDeleteTextures(_GUIcolorTextureId);
                _GUIcolorTextureId = -1;
            }

            if (_GUIframeBufferId != -1)
            {
                GL30.glDeleteFramebuffers(_GUIframeBufferId);
                _GUIframeBufferId = -1;
            }
            if( superSampleSupported )
            {

	            // Supersampled result FBO(s)
	            if (_Lanczos_GUIdepthRenderBufferId1 != -1)
	            {
	                GL30.glDeleteRenderbuffers(_Lanczos_GUIdepthRenderBufferId1);
	                _Lanczos_GUIdepthRenderBufferId1 = -1;
	            }
	
	            if (_Lanczos_GUIcolorTextureId1 != -1)
	            {
	                GL11.glDeleteTextures(_Lanczos_GUIcolorTextureId1);
	                _Lanczos_GUIcolorTextureId1 = -1;
	            }
	
	            if (_Lanczos_GUIframeBufferId1 != -1)
	            {
	                GL30.glDeleteFramebuffers(_Lanczos_GUIframeBufferId1);
	                _Lanczos_GUIframeBufferId1 = -1;
	            }
	
	            if (_Lanczos_GUIdepthRenderBufferId2 != -1)
	            {
	                GL30.glDeleteRenderbuffers(_Lanczos_GUIdepthRenderBufferId2);
	                _Lanczos_GUIdepthRenderBufferId2 = -1;
	            }
	
	            if (_Lanczos_GUIcolorTextureId2 != -1)
	            {
	                GL11.glDeleteTextures(_Lanczos_GUIcolorTextureId2);
	                _Lanczos_GUIcolorTextureId2 = -1;
	            }
	
	            if (_Lanczos_GUIframeBufferId2 != -1)
	            {
	                GL30.glDeleteFramebuffers(_Lanczos_GUIframeBufferId2);
	                _Lanczos_GUIframeBufferId2 = -1;
	            }
	
	            destroyVBO();
            }
            _position_id = -1;
            _inputTextureCoordinate_id = -1;
        }

        if (!_FBOInitialised)
        {
            FBOParams mainFboParams = null;
            System.out.println("Width: " + this.mc.displayWidth + ", Height: " + this.mc.displayHeight);
            if ( superSampleSupported && this.mc.gameSettings.useSupersample)
            {
                mainFboParams = createFBO(false, (int)ceil(this.mc.displayWidth * eyeRenderParams._renderScale * this.mc.gameSettings.superSampleScaleFactor), (int)ceil(this.mc.displayHeight * eyeRenderParams._renderScale * this.mc.gameSettings.superSampleScaleFactor));
            }
            else
            {
                mainFboParams = createFBO(false, (int)ceil(this.mc.displayWidth * eyeRenderParams._renderScale), (int)ceil(this.mc.displayHeight * eyeRenderParams._renderScale));
            }
            mc.checkGLError("FBO create");

            // Main FBO
            _frameBufferId = mainFboParams._frameBufferId;
            _colorTextureId = mainFboParams._colorTextureId;
            _depthRenderBufferId = mainFboParams._depthRenderBufferId;
            if (this.mc.gameSettings.useChromaticAbCorrection)
            {
                _shaderProgramId = initOculusShaders(OCULUS_BASIC_VERTEX_SHADER, OCULUS_DISTORTION_FRAGMENT_SHADER_WITH_CHROMATIC_ABERRATION_CORRECTION, false);
            }
            else
            {
                _shaderProgramId = initOculusShaders(OCULUS_BASIC_VERTEX_SHADER, OCULUS_DISTORTION_FRAGMENT_SHADER_NO_CHROMATIC_ABERRATION_CORRECTION, false);
            }
            mc.checkGLError("FBO init shader");

            // GUI FBO
            FBOParams GUIFboParams = createFBO(false, this.mc.displayWidth, this.mc.displayHeight);
            mc.checkGLError("GUI FBO create");

            _GUIframeBufferId = GUIFboParams._frameBufferId;
            _GUIcolorTextureId = GUIFboParams._colorTextureId;
            _GUIdepthRenderBufferId = GUIFboParams._depthRenderBufferId;
            _GUIshaderProgramId = initOculusShaders(OCULUS_BASIC_VERTEX_SHADER, OCULUS_BASIC_FRAGMENT_SHADER, false);
            mc.checkGLError("FBO init GUI shader");
            
            if( superSampleSupported )
            {
	            // Lanczos downsample FBOs
	            FBOParams lanczosInitialFboParams = new FBOParams();
	            FBOParams lanczosAfter1stPassFboParams = new FBOParams();
	            if (this.mc.gameSettings.useSupersample)
	            {
	                lanczosInitialFboParams = createFBO(false, (int)ceil(this.mc.displayWidth * this.mc.gameSettings.superSampleScaleFactor), (int)ceil(this.mc.displayHeight * this.mc.gameSettings.superSampleScaleFactor));
	                lanczosAfter1stPassFboParams = createFBO(false, (int)ceil(this.mc.displayWidth), (int)ceil(this.mc.displayHeight * this.mc.gameSettings.superSampleScaleFactor));
	            }
	
	            mc.checkGLError("Lanczos FBO create");
	
	            _Lanczos_GUIframeBufferId1 = lanczosInitialFboParams._frameBufferId;
	            _Lanczos_GUIcolorTextureId1 = lanczosInitialFboParams._colorTextureId;
	            _Lanczos_GUIdepthRenderBufferId1 = lanczosInitialFboParams._depthRenderBufferId;
	
	            _Lanczos_GUIframeBufferId2 = lanczosAfter1stPassFboParams._frameBufferId;
	            _Lanczos_GUIcolorTextureId2 = lanczosAfter1stPassFboParams._colorTextureId;
	            _Lanczos_GUIdepthRenderBufferId2 = lanczosAfter1stPassFboParams._depthRenderBufferId;
	
	            if (this.mc.gameSettings.useSupersample)
	            {
	                _Lanczos_shaderProgramId = initOculusShaders(LANCZOS_SAMPLER_VERTEX_SHADER, LANCZOS_SAMPLER_FRAGMENT_SHADER, true);
	                mc.checkGLError("@1");
	
	
	                GL20.glValidateProgram(_Lanczos_shaderProgramId);
	
	                mc.checkGLError("FBO init Lanczos shader");
	
	                setupQuad();
	
	                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	            }
	            else
	            {
	                _Lanczos_shaderProgramId = -1;
	            }
            }



            _FBOInitialised = true;
        }

        //Used by fog comparison, 3rd person camera/block collision detection, and getMouseOver
        renderViewEntityX = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * (double)renderPartialTicks;
        renderViewEntityY = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * (double)renderPartialTicks;
        renderViewEntityZ = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * (double)renderPartialTicks;
        //Looks like cloudFog is no longer used
        //this.cloudFog = this.mc.renderGlobal.hasCloudFog(renderViewEntityX, renderViewEntityY, renderViewEntityZ, renderPartialTicks);

        for (int renderSceneNumber = 0; renderSceneNumber < 2; ++renderSceneNumber)
        {
            if (mc.gameSettings.useDistortion)
            {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, _frameBufferId);
                mc.checkGLError("fb");
            }
            else
            {
                eyeRenderParams._renderScale = 1.0f;
            }

            if (this.mc.gameSettings.anaglyph)
            {
                anaglyphField = renderSceneNumber;

                if (anaglyphField == 0)
                {
                    GL11.glColorMask(false, true, true, false);
                }
                else
                {
                    GL11.glColorMask(true, false, false, false);
                }
            }
            else
            {
                GL11.glColorMask(true, true, true, true);
            }

            this.mc.mcProfiler.endStartSection("clear");
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            mc.checkGLError("scissor");

            if (renderSceneNumber == 0)
            {
                // Left eye
                GL11.glViewport((int)ceil(eyeRenderParams._leftViewPortX * eyeRenderParams._renderScale),
                        (int)ceil(eyeRenderParams._leftViewPortY * eyeRenderParams._renderScale),
                        (int)ceil(eyeRenderParams._leftViewPortW * eyeRenderParams._renderScale),
                        (int)ceil(eyeRenderParams._leftViewPortH * eyeRenderParams._renderScale));
                mc.checkGLError("56");

                GL11.glScissor((int)ceil(eyeRenderParams._leftViewPortX * eyeRenderParams._renderScale),
                        (int)ceil(eyeRenderParams._leftViewPortY * eyeRenderParams._renderScale),
                        (int)ceil(eyeRenderParams._leftViewPortW * eyeRenderParams._renderScale),
                        (int)ceil(eyeRenderParams._leftViewPortH * eyeRenderParams._renderScale));
                mc.checkGLError("34");
            }
            else
            {
                // Right eye
                GL11.glViewport((int)ceil(eyeRenderParams._rightViewPortX * eyeRenderParams._renderScale),
                                (int)ceil(eyeRenderParams._rightViewPortY * eyeRenderParams._renderScale),
                                (int)ceil(eyeRenderParams._rightViewPortW * eyeRenderParams._renderScale),
                                (int)ceil(eyeRenderParams._rightViewPortH * eyeRenderParams._renderScale));

                mc.checkGLError("11");
                GL11.glScissor((int)ceil(eyeRenderParams._rightViewPortX * eyeRenderParams._renderScale),
                               (int)ceil(eyeRenderParams._rightViewPortY * eyeRenderParams._renderScale),
                               (int)ceil(eyeRenderParams._rightViewPortW * eyeRenderParams._renderScale),
                               (int)ceil(eyeRenderParams._rightViewPortH * eyeRenderParams._renderScale));
            }

            mc.checkGLError("FBO viewport / scissor setup");

            GL11.glClear (GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            this.updateFogColor(renderPartialTicks);
            GL11.glClearColor (fogColorRed, fogColorGreen, fogColorBlue, 0.5f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glShadeModel(GL11.GL_SMOOTH);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glCullFace(GL11.GL_BACK);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            mc.checkGLError("FBO init");

            this.mc.mcProfiler.startSection("lightTex");
            if (this.lightmapUpdateNeeded)
            {
                this.updateLightmap(renderPartialTicks);
            }

            this.mc.mcProfiler.endStartSection("camera");

            //transform camera with pitch,yaw,roll + neck model + game effects 
            this.setupCameraTransform(renderPartialTicks, renderSceneNumber);
            
            
            ActiveRenderInfo.updateRenderInfo(this.mc.thePlayer, this.mc.gameSettings.thirdPersonView == 2);
            this.mc.mcProfiler.endStartSection("frustrum");
            ClippingHelperImpl.getInstance(); // setup clip, using current modelview / projection matrices

            if (!Config.isSkyEnabled() && !Config.isSunMoonEnabled() && !Config.isStarsEnabled())
            {
                GL11.glDisable(GL11.GL_BLEND);
            }
            else
            {
                this.setupFog(-1, renderPartialTicks);
                this.mc.mcProfiler.endStartSection("sky");
                renderGlobal.renderSky(renderPartialTicks);
            }

            GL11.glEnable(GL11.GL_FOG);
            this.setupFog(1, renderPartialTicks);

            if (this.mc.gameSettings.ambientOcclusion != 0)
            {
                GL11.glShadeModel(GL11.GL_SMOOTH);
            }

            this.mc.mcProfiler.endStartSection("culling");
            Frustrum frustrum = new Frustrum();
            frustrum.setPosition(renderViewEntityX , renderViewEntityY, renderViewEntityZ);


            this.mc.renderGlobal.clipRenderersByFrustum(frustrum, renderPartialTicks);

            if (renderSceneNumber == 0 )
            {
                this.mc.mcProfiler.endStartSection("updatechunks");

                while (!this.mc.renderGlobal.updateRenderers(renderViewEntity, false) && nextFrameTime != 0L)
                {
                    long var15 = nextFrameTime - System.nanoTime();

                    if (var15 < 0L || var15 > 1000000000L)
                    {
                        break;
                    }
                }
            }

            if (renderViewEntity.posY < 128.0D)
            {
                this.renderCloudsCheck(renderGlobal, renderPartialTicks);
            }

            this.mc.mcProfiler.endStartSection("prepareterrain");
            this.setupFog(0, renderPartialTicks);
            GL11.glEnable(GL11.GL_FOG);
            this.mc.renderEngine.bindTexture("/terrain.png");
            RenderHelper.disableStandardItemLighting();
            this.mc.mcProfiler.endStartSection("terrain");
            renderGlobal.sortAndRender(renderViewEntity, 0, (double) renderPartialTicks);
            GL11.glShadeModel(GL11.GL_FLAT);
            boolean var16 = Reflector.ForgeHooksClient.exists();
            EntityPlayer var18;

            if (this.debugViewDirection == 0)
            {
                RenderHelper.enableStandardItemLighting();
                this.mc.mcProfiler.endStartSection("entities");

                if (var16)
                {
                    Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, new Object[] {Integer.valueOf(0)});
                }

                renderGlobal.renderEntities(renderViewEntity.getPosition(renderPartialTicks), frustrum, renderPartialTicks);

                if (var16)
                {
                    Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, new Object[] {Integer.valueOf(-1)});
                }
                this.enableLightmap((double) renderPartialTicks);
                this.mc.mcProfiler.endStartSection("litParticles");
                effectRenderer.renderLitParticles(renderViewEntity, renderPartialTicks);
                RenderHelper.disableStandardItemLighting();
                this.setupFog(0, renderPartialTicks);
                this.mc.mcProfiler.endStartSection("particles");
                effectRenderer.renderParticles(renderViewEntity, renderPartialTicks);
                this.disableLightmap((double) renderPartialTicks);
                                                                                                                                                        // TODO: Always render outline?
                if (this.mc.objectMouseOver != null && renderViewEntity.isInsideOfMaterial(Material.water) && renderViewEntity instanceof EntityPlayer && !this.mc.gameSettings.hideGUI)
                {
                    var18 = (EntityPlayer)renderViewEntity;
                    GL11.glDisable(GL11.GL_ALPHA_TEST);
                    this.mc.mcProfiler.endStartSection("outline");

                    if (!var16 || !Reflector.callBoolean(Reflector.ForgeHooksClient_onDrawBlockHighlight, new Object[] {renderGlobal, var18, this.mc.objectMouseOver, Integer.valueOf(0), var18.inventory.getCurrentItem(), Float.valueOf(renderPartialTicks)}))
                    {
                        renderGlobal.drawBlockBreaking(var18, this.mc.objectMouseOver, 0, var18.inventory.getCurrentItem(), renderPartialTicks);
                        if (!this.mc.gameSettings.hideGUI)
                        {
                            renderGlobal.drawSelectionBox(var18, this.mc.objectMouseOver, 0, var18.inventory.getCurrentItem(), renderPartialTicks);
                        }
                    }
                    GL11.glEnable(GL11.GL_ALPHA_TEST);
                }
            }

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(true);
            this.setupFog(0, renderPartialTicks);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            this.mc.renderEngine.bindTexture("/terrain.png");
            WrUpdates.resumeBackgroundUpdates();

            if (Config.isWaterFancy())
            {
                this.mc.mcProfiler.endStartSection("water");

                if (this.mc.gameSettings.ambientOcclusion != 0)
                {
                    GL11.glShadeModel(GL11.GL_SMOOTH);
                }

                GL11.glColorMask(false, false, false, false);
                int var17 = renderGlobal.renderAllSortedRenderers(1, (double)renderPartialTicks);

                if (this.mc.gameSettings.anaglyph)
                {
                    if (anaglyphField == 0)
                    {
                        GL11.glColorMask(false, true, true, true);
                    }
                    else
                    {
                        GL11.glColorMask(true, false, false, true);
                    }
                }
                else
                {
                    GL11.glColorMask(true, true, true, true);
                }

                if (var17 > 0)
                {
                    renderGlobal.renderAllSortedRenderers(1, (double)renderPartialTicks);
                }

                GL11.glShadeModel(GL11.GL_FLAT);
            }
            else
            {
                this.mc.mcProfiler.endStartSection("water");
                renderGlobal.renderAllSortedRenderers( 1, (double) renderPartialTicks);
            }

            WrUpdates.pauseBackgroundUpdates();
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_BLEND);
                                                                                        // TODO: Always render selection boxes?
            if (this.cameraZoom == 1.0D && renderViewEntity instanceof EntityPlayer && !this.mc.gameSettings.hideGUI && this.mc.objectMouseOver != null && !renderViewEntity.isInsideOfMaterial(Material.water))
            {
                var18 = (EntityPlayer)renderViewEntity;
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                this.mc.mcProfiler.endStartSection("outline");

                if (!var16 || !Reflector.callBoolean(Reflector.ForgeHooksClient_onDrawBlockHighlight, new Object[] {renderGlobal, var18, this.mc.objectMouseOver, Integer.valueOf(0), var18.inventory.getCurrentItem(), Float.valueOf(renderPartialTicks)}))
                {
                    renderGlobal.drawBlockBreaking(var18, this.mc.objectMouseOver, 0, var18.inventory.getCurrentItem(), renderPartialTicks );

                    if (!this.mc.gameSettings.hideGUI)
                    {
                        renderGlobal.drawSelectionBox(var18, this.mc.objectMouseOver, 0, var18.inventory.getCurrentItem(), renderPartialTicks );
                    }
                }
                GL11.glEnable(GL11.GL_ALPHA_TEST);
            }

            this.mc.mcProfiler.endStartSection("destroyProgress");
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            renderGlobal.drawBlockDamageTexture(Tessellator.instance, renderViewEntity, renderPartialTicks);
            GL11.glDisable(GL11.GL_BLEND);
            this.mc.mcProfiler.endStartSection("weather");
            this.renderRainSnow(renderPartialTicks);
            GL11.glDisable(GL11.GL_FOG);

            if (renderViewEntity.posY >= 128.0D)
            {
                this.renderCloudsCheck(renderGlobal, renderPartialTicks);
            }

            if (var16)
            {
                this.mc.mcProfiler.endStartSection("FRenderLast");
                Reflector.callVoid(Reflector.ForgeHooksClient_dispatchRenderLast, new Object[] {renderGlobal, Float.valueOf(renderPartialTicks)});
            }

            // Add GUI overlay
            if (!this.mc.gameSettings.hideGUI || this.mc.currentScreen != null  )
            {
                
            	//Draw crosshair
            	

                GL11.glClear( GL11.GL_DEPTH_BUFFER_BIT);
                GL11.glPointSize(5.0f);
                GL11.glBegin(GL11.GL_POINTS);
                GL11.glColor3f(1.0f, 1.0f, 1.0f);
                pointX = 0;
                pointY = 0;
                pointZ = 0;
                GL11.glVertex3f(pointX, pointY, pointZ);
                GL11.glEnd();
	            this.mc.mcProfiler.endStartSection("crosshair");
	            this.mc.renderEngine.bindTexture("/gui/icons.png");

		        MovingObjectPosition crossPos = this.mc.objectMouseOver;
		        if( crossPos == null )
		        {
		        	crossPos = this.mc.renderViewEntity.rayTrace(128, renderPartialTicks);
		        }
		        if( crossPos != null )
		        {
		        	float crossX = (float)(crossPos.hitVec.xCoord - renderViewEntityX);
		        	float crossY = (float)(crossPos.hitVec.yCoord - renderViewEntityY);
		        	float crossZ = (float)(crossPos.hitVec.zCoord - renderViewEntityZ);

			        float var7 = 0.00390625F;
			        float var8 = 0.00390625F;
			        Tessellator.instance.startDrawingQuads();
			        Tessellator.instance.addVertexWithUV(- 1, + 1, 0,  0     , 16* var8);
			        Tessellator.instance.addVertexWithUV(+ 1, + 1, 0, 16*var7, 16* var8);
			        Tessellator.instance.addVertexWithUV(+ 1, - 1, 0, 16*var7, 0       );
			        Tessellator.instance.addVertexWithUV(- 1, - 1, 0, 0      , 0       );

		            GL11.glPushMatrix();
		            GL11.glTranslatef(crossX, crossY, crossZ);
		            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
		            GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
		            GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
		            float scale = 0.05f*(float)Math.sqrt((crossX*crossX + crossY*crossY + crossZ*crossZ));
		            GL11.glScalef(-scale, -scale, scale);
		            GL11.glDisable(GL11.GL_LIGHTING);
		            GL11.glEnable(GL11.GL_DEPTH_TEST);
		            GL11.glEnable(GL11.GL_BLEND);
		            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f); //white crosshair
			        Tessellator.instance.draw();
			        GL11.glDisable(GL11.GL_BLEND);
			        GL11.glPopMatrix();
		        }

                
                GL11.glMatrixMode(GL11.GL_MODELVIEW);

                GL11.glDisable(GL11.GL_SCISSOR_TEST);

                // Switch to GUI FBO
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, _GUIframeBufferId);

                // Set viewport
                GL11.glViewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);

                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glClear (GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                this.updateFogColor(renderPartialTicks);
                if (this.mc.gameSettings.useHudOpacity)
                {
                    GL11.glClearColor (fogColorRed, fogColorGreen, fogColorBlue, 0.11f);
                }
                else
                {
                    GL11.glClearColor (1.0f, 1.0f, 1.0f, 0.0f);
                }
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_CULL_FACE);
                GL11.glEnable(GL11.GL_TEXTURE_2D); // TODO: Anything else to init for FBO?
                GL11.glShadeModel(GL11.GL_SMOOTH);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
                GL11.glCullFace(GL11.GL_BACK);

                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glLoadIdentity();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);

                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glColorMask(true, true, true, true);
                mc.checkGLError("FBO init");

                // TODO: set correct mouse x and y params
                int width = eyeRenderParams._leftViewPortW;
                int height = eyeRenderParams._leftViewPortH;
                float renderScale = 1.0f;

                ScaledResolution scaledResolution = new ScaledResolution(this.mc.gameSettings, this.mc.displayWidth, this.mc.displayHeight, true);    // works
                  _GUIscaledWidth = scaledResolution.getScaledWidth();
                _GUIscaledHeight = scaledResolution.getScaledHeight();
                _GUIscaleFactor = scaledResolution.getScaleFactor();

                if (renderSceneNumber == 0)
                {
                    this.setCorrectedMouse(Mouse.getEventX(), Mouse.getEventY(), this.mc.displayWidth, this.mc.displayHeight);
                }

                int mouseXNow = mouseX;
                int mouseYNow = mouseY;

                // Use only half screen width
                if (mouseXNow > (this.mc.displayWidth / 2))
                {
                    mouseXNow -= this.mc.displayWidth / 2;   // TODO: Correct mouse position for half width viewport
                }

                // Works
                int scaledMouseX = (int)ceil((mouseXNow * _GUIscaledWidth / (this.mc.displayWidth / 2) /** renderScale*/));
                int scaledMouseY = (int)ceil(_GUIscaledHeight - ((mouseYNow * _GUIscaledHeight / this.mc.displayHeight - 1) /* renderScale*/));

                // Setup ortho view
                this.setupOverlayRendering();

                if (!this.mc.gameSettings.hideGUI/* && this.mc.currentScreen == null*/)
                {
                    this.mc.ingameGUI.renderGameOverlay(renderPartialTicks, oculusRift,
                            this.mc.displayWidth, this.mc.displayHeight, renderScale, width, height, _GUIscaleFactor, _GUIscaledWidth, _GUIscaledHeight, mouseXNow, mouseYNow, scaledMouseX, scaledMouseY);
                    guiAchievement.updateAchievementWindow(oculusRift, eyeRenderParams, renderSceneNumber);
                }

                if (this.mc.currentScreen != null)
                {
                    this.mc.currentScreen.drawScreen(scaledMouseX, scaledMouseY, renderPartialTicks);

                    // TODO: Add mouse pointer per viewport
                    GL11.glDisable(GL11.GL_BLEND);
                    this.mc.mcProfiler.endStartSection("mouse pointer");
                    this.mc.renderEngine.bindTexture("/gui/icons.png");
                    float prevZ = this.mc.ingameGUI.zLevel;
                    this.mc.ingameGUI.zLevel = 999.0f;
                    this.mc.ingameGUI.drawTexturedModalRect(scaledMouseX - 7, scaledMouseY - 7, 0, 0, 16, 16);
                    this.mc.ingameGUI.zLevel = prevZ;
                }

                // Switch back to previous framebuffer
                if (mc.gameSettings.useDistortion)
                {
                    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, _frameBufferId);
                    mc.checkGLError("GUI fb");
                }
                else
                {
                    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                    mc.checkGLError("GUI fb");
                }

                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glEnable(GL11.GL_BLEND); // Allow GUI transparency!
                //GL11.glDisable(GL11.GL_ALPHA_TEST);

                if (renderSceneNumber == 0)
                {
                    // Left eye
                    GL11.glViewport((int)ceil(eyeRenderParams._leftViewPortX * eyeRenderParams._renderScale),
                            (int)ceil(eyeRenderParams._leftViewPortY * eyeRenderParams._renderScale),
                            (int)ceil(eyeRenderParams._leftViewPortW * eyeRenderParams._renderScale),
                            (int)ceil(eyeRenderParams._leftViewPortH * eyeRenderParams._renderScale));
                    mc.checkGLError("56");

                    GL11.glScissor((int)ceil(eyeRenderParams._leftViewPortX * eyeRenderParams._renderScale),
                            (int)ceil(eyeRenderParams._leftViewPortY * eyeRenderParams._renderScale),
                            (int)ceil(eyeRenderParams._leftViewPortW * eyeRenderParams._renderScale),
                            (int)ceil(eyeRenderParams._leftViewPortH * eyeRenderParams._renderScale));
                    mc.checkGLError("34");
                }
                else
                {
                    // Right eye
                    GL11.glViewport((int)ceil(eyeRenderParams._rightViewPortX * eyeRenderParams._renderScale),
                            (int)ceil(eyeRenderParams._rightViewPortY * eyeRenderParams._renderScale),
                            (int)ceil(eyeRenderParams._rightViewPortW * eyeRenderParams._renderScale),
                            (int)ceil(eyeRenderParams._rightViewPortH * eyeRenderParams._renderScale));

                    mc.checkGLError("11");
                    GL11.glScissor((int)ceil(eyeRenderParams._rightViewPortX * eyeRenderParams._renderScale),
                            (int)ceil(eyeRenderParams._rightViewPortY * eyeRenderParams._renderScale),
                            (int)ceil(eyeRenderParams._rightViewPortW * eyeRenderParams._renderScale),
                            (int)ceil(eyeRenderParams._rightViewPortH * eyeRenderParams._renderScale));
                }

                // Set up perspective view
                this.setupOverlayRendering(_GUIscaledWidth, _GUIscaledHeight, _GUIscaleFactor, oculusRift, eyeRenderParams, this.mc.renderViewEntity, renderPartialTicks, renderSceneNumber);

                int textureUnit = GL13.GL_TEXTURE0; //OpenGlHelper.defaultTexUnit;
                OpenGlHelper.setActiveTexture(textureUnit);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, _GUIcolorTextureId);

                // Set basic GUI shader in place & set uniforms
                ARBShaderObjects.glUseProgramObjectARB(_GUIshaderProgramId);
                ARBShaderObjects.glUniform1iARB(ARBShaderObjects.glGetUniformLocationARB(_GUIshaderProgramId, "bgl_RenderTexture"), 0);

                GL11.glColor3f(1, 1, 1);                                               // set the color to white

                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                drawQuad2(this.mc.displayWidth, this.mc.displayHeight, this.mc.gameSettings.hudScale);

                // Stop shader use
                ARBShaderObjects.glUseProgramObjectARB(0);

                //GL11.glFlush();
                GL11.glDisable(GL11.GL_BLEND);

            }


        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_BLEND);

        if (mc.gameSettings.useDistortion)
        {
            mc.checkGLError("Before distortion");

            // Bind the texture
            int textureUnit = GL13.GL_TEXTURE0; //OpenGlHelper.defaultTexUnit;
            OpenGlHelper.setActiveTexture(textureUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, _colorTextureId);

            if ( superSampleSupported && this.mc.gameSettings.useSupersample)
            {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, _Lanczos_GUIframeBufferId1);    // switch to rendering on our to-be-lanczos sampled framebuffer
            }
            else
            {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);                    // switch to rendering on the framebuffer
            }

            GL11.glClearColor (1.0f, 1.0f, 1.0f, 0.5f);
            GL11.glClearDepth(1.0D);
            GL11.glClear (GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);            // Clear Screen And Depth Buffer on the framebuffer to black

            // Render onto the entire screen framebuffer
            if (superSampleSupported && this.mc.gameSettings.useSupersample)
            {
                GL11.glViewport(0, 0, (int)ceil(this.mc.displayWidth * this.mc.gameSettings.superSampleScaleFactor), (int)ceil(this.mc.displayHeight * this.mc.gameSettings.superSampleScaleFactor));
            }
            else
            {
                GL11.glViewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
            }
            mc.checkGLError("3");

            // Set the distortion shader as in use
            ARBShaderObjects.glUseProgramObjectARB(_shaderProgramId);

            HMDInfo hmdInfo = oculusRift.getHMDInfo();

            float lw = 0;
            float lh = 0;
            float lx = 0;
            float ly = 0;
            float rw = 0;
            float rh = 0;
            float rx = 0;
            float ry = 0;

            if (this.mc.gameSettings.useDistortion && superSampleSupported && this.mc.gameSettings.useSupersample)
            {
                lw = eyeRenderParams._leftViewPortW / ((float)this.mc.displayWidth * this.mc.gameSettings.superSampleScaleFactor);
                lh = eyeRenderParams._leftViewPortH / ((float)this.mc.displayHeight * this.mc.gameSettings.superSampleScaleFactor);
                lx = eyeRenderParams._leftViewPortX / ((float)this.mc.displayWidth * this.mc.gameSettings.superSampleScaleFactor);
                ly = eyeRenderParams._leftViewPortY / ((float)this.mc.displayHeight * this.mc.gameSettings.superSampleScaleFactor);
                rw = eyeRenderParams._rightViewPortW / ((float)this.mc.displayWidth * this.mc.gameSettings.superSampleScaleFactor);
                rh = eyeRenderParams._rightViewPortH / ((float)this.mc.displayHeight * this.mc.gameSettings.superSampleScaleFactor);
                rx = eyeRenderParams._rightViewPortX / ((float)this.mc.displayWidth * this.mc.gameSettings.superSampleScaleFactor);
                ry = eyeRenderParams._rightViewPortY / ((float)this.mc.displayHeight * this.mc.gameSettings.superSampleScaleFactor);
            }
            else
            {
                lw = eyeRenderParams._leftViewPortW / (float)this.mc.displayWidth;
                lh = eyeRenderParams._leftViewPortH / (float)this.mc.displayHeight;
                lx = eyeRenderParams._leftViewPortX / (float)this.mc.displayWidth;
                ly = eyeRenderParams._leftViewPortY / (float)this.mc.displayHeight;
                rw = eyeRenderParams._rightViewPortW / (float)this.mc.displayWidth;
                rh = eyeRenderParams._rightViewPortH / (float)this.mc.displayHeight;
                rx = eyeRenderParams._rightViewPortX / (float)this.mc.displayWidth;
                ry = eyeRenderParams._rightViewPortY / (float)this.mc.displayHeight;
            }

            float aspect = (float)eyeRenderParams._leftViewPortW / (float)eyeRenderParams._leftViewPortH;

            float leftLensCenterX = lx + (lw + eyeRenderParams._XCenterOffset * 0.5f) * 0.5f;
            float leftLensCenterY = ly + lh * 0.5f;
            float rightLensCenterX = rx + (rw + -eyeRenderParams._XCenterOffset * 0.5f) * 0.5f;
            float rightLensCenterY = ry + rh * 0.5f;

            float leftScreenCenterX = lx + lw * 0.5f;
            float leftScreenCenterY = ly + lh * 0.5f;
            float rightScreenCenterX = rx + rw * 0.5f;
            float rightScreenCenterY = ry + rh * 0.5f;

            float scaleFactor = 1.0f / eyeRenderParams._renderScale;
            float scaleX = (lw / 2) * scaleFactor;
            float scaleY = (lh / 2) * scaleFactor * aspect;
            float scaleInX = 2 / lw;
            float scaleInY = (2 / lh) / aspect;

            // Set up the fragment shader uniforms
            ARBShaderObjects.glUniform1iARB(ARBShaderObjects.glGetUniformLocationARB(_shaderProgramId, "bgl_RenderTexture"), 0);
            if ( superSampleSupported && this.mc.gameSettings.useSupersample)
            {
                ARBShaderObjects.glUniform1iARB(ARBShaderObjects.glGetUniformLocationARB(_shaderProgramId, "half_screenWidth"), (int)ceil(((float)this.mc.displayWidth * this.mc.gameSettings.superSampleScaleFactor) / 2.0f));
            }
            else
            {
                ARBShaderObjects.glUniform1iARB(ARBShaderObjects.glGetUniformLocationARB(_shaderProgramId, "half_screenWidth"), this.mc.displayWidth / 2);
            }
            ARBShaderObjects.glUniform2fARB(ARBShaderObjects.glGetUniformLocationARB(_shaderProgramId, "LeftLensCenter"), leftLensCenterX, leftLensCenterY);
            ARBShaderObjects.glUniform2fARB(ARBShaderObjects.glGetUniformLocationARB(_shaderProgramId, "RightLensCenter"), rightLensCenterX, rightLensCenterY);
            ARBShaderObjects.glUniform2fARB(ARBShaderObjects.glGetUniformLocationARB(_shaderProgramId, "LeftScreenCenter"), leftScreenCenterX, leftScreenCenterY);
            ARBShaderObjects.glUniform2fARB(ARBShaderObjects.glGetUniformLocationARB(_shaderProgramId, "RightScreenCenter"), rightScreenCenterX, rightScreenCenterY);
            ARBShaderObjects.glUniform2fARB(ARBShaderObjects.glGetUniformLocationARB(_shaderProgramId, "Scale"), scaleX, scaleY);
            ARBShaderObjects.glUniform2fARB(ARBShaderObjects.glGetUniformLocationARB(_shaderProgramId, "ScaleIn"), scaleInX, scaleInY);
            ARBShaderObjects.glUniform4fARB(ARBShaderObjects.glGetUniformLocationARB(_shaderProgramId, "HmdWarpParam"), hmdInfo.DistortionK[0], hmdInfo.DistortionK[1], hmdInfo.DistortionK[2], hmdInfo.DistortionK[3]);
            ARBShaderObjects.glUniform4fARB(ARBShaderObjects.glGetUniformLocationARB(_shaderProgramId, "ChromAbParam"), hmdInfo.ChromaticAb[0], hmdInfo.ChromaticAb[1], hmdInfo.ChromaticAb[2], hmdInfo.ChromaticAb[3]);

            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);

            GL11.glTranslatef (0.0f, 0.0f, -0.7f);                               // Translate 6 Units Into The Screen and then rotate
            GL11.glColor3f(1, 1, 1);                                               // set the color to white

            drawQuad();                                                      // draw the box

            // Stop shader use
            ARBShaderObjects.glUseProgramObjectARB(0);

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();

            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

            mc.checkGLError("After distortion");
        }

        if (superSampleSupported && this.mc.gameSettings.useSupersample)
        {
            // Now switch to 1st pass target framebuffer
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, _Lanczos_GUIframeBufferId2);

            // Bind the texture
            int textureUnit = GL13.GL_TEXTURE0; //OpenGlHelper.defaultTexUnit;
            OpenGlHelper.setActiveTexture(textureUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, _Lanczos_GUIcolorTextureId1);


            GL11.glClearColor (0.0f, 0.0f, 1.0f, 0.5f);
            GL11.glClearDepth(1.0D);
            GL11.glClear (GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);            // Clear Screen And Depth Buffer on the framebuffer to black

            // Render onto the entire screen framebuffer
            GL11.glViewport(0, 0, this.mc.displayWidth, (int)ceil((float)this.mc.displayHeight * this.mc.gameSettings.superSampleScaleFactor));
            mc.checkGLError("3");


            // Set the downsampling shader as in use
            ARBShaderObjects.glUseProgramObjectARB(_Lanczos_shaderProgramId);
            this.mc.checkGLError("UseLanczos");

            // Set up the fragment shader uniforms
            this.mc.checkGLError("lanzosLoc");
            ARBShaderObjects.glUniform1fARB(ARBShaderObjects.glGetUniformLocationARB(_Lanczos_shaderProgramId, "texelWidthOffset"), 1.0f / (3.0f * (float)this.mc.displayWidth));// * this.mc.gameSettings.superSampleScaleFactor);
            this.mc.checkGLError("lanzosUni1");
            ARBShaderObjects.glUniform1fARB(ARBShaderObjects.glGetUniformLocationARB(_Lanczos_shaderProgramId, "texelHeightOffset"), 0.0f);
            this.mc.checkGLError("lanzosUni2");
            ARBShaderObjects.glUniform1iARB(ARBShaderObjects.glGetUniformLocationARB(_Lanczos_shaderProgramId, "inputImageTexture"), 0);

            // Pass 1

            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            // Bind to the VAO that has all the information about the vertices
            GL30.glBindVertexArray(vaoId);
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            // Bind to the index VBO that has all the information about the order of the vertices
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId);

            // Draw the vertices
            GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_BYTE, 0);

            // Put everything back to default (deselect)
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
            GL30.glBindVertexArray(0);

            // Pass 2
            // Now switch to 2nd pass screen framebuffer
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, _Lanczos_GUIcolorTextureId2);

            GL11.glViewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
            GL11.glClearColor (0.0f, 0.0f, 1.0f, 0.5f);
            GL11.glClearDepth(1.0D);
            GL11.glClear (GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            // Bind the texture
            GL13.glActiveTexture(GL13.GL_TEXTURE0);

            // Set up the fragment shader uniforms for pass 2
            this.mc.checkGLError("lanzosLoc");
            ARBShaderObjects.glUniform1fARB(ARBShaderObjects.glGetUniformLocationARB(_Lanczos_shaderProgramId, "texelWidthOffset"), 0.0f);// * this.mc.gameSettings.superSampleScaleFactor);
            this.mc.checkGLError("lanzosUni1");
            ARBShaderObjects.glUniform1fARB(ARBShaderObjects.glGetUniformLocationARB(_Lanczos_shaderProgramId, "texelHeightOffset"), 1.0f / (3.0f * (float)this.mc.displayHeight));
            this.mc.checkGLError("lanzosUni2");
            ARBShaderObjects.glUniform1iARB(ARBShaderObjects.glGetUniformLocationARB(_Lanczos_shaderProgramId, "inputImageTexture"), 0);

            // Bind to the VAO that has all the information about the vertices
            GL30.glBindVertexArray(vaoId);
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            // Bind to the index VBO that has all the information about the order of the vertices
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId);

            // Draw the vertices
            GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_BYTE, 0);

            // Put everything back to default (deselect)
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
            GL30.glBindVertexArray(0);

            // Stop shader use
            ARBShaderObjects.glUseProgramObjectARB(0);
            this.mc.checkGLError("loopCycle");
        }

        GL11.glColorMask(true, true, true, false);
        GL11.glFlush();
        this.mc.mcProfiler.endSection();
    }

    private void destroyVBO()
    {
        // Select the VAO
        GL30.glBindVertexArray(vaoId);

        // Disable the VBO index from the VAO attributes list
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);

        // Delete the vertex VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glDeleteBuffers(vboId);

        // Delete the index VBO
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL15.glDeleteBuffers(vboiId);

        // Delete the VAO
        GL30.glBindVertexArray(0);
        GL30.glDeleteVertexArrays(vaoId);

        this.mc.checkGLError("destroyVBO");
    }

    private void setupQuad()
    {
        // We'll define our quad using 4 vertices of the custom 'TexturedVertex' class
        TexturedVertex v0 = new TexturedVertex();
        v0.setXYZ(-1.0f, 1.0f, 0); v0.setRGB(1, 0, 0); v0.setST(0, 1);
        TexturedVertex v1 = new TexturedVertex();
        v1.setXYZ(-1.0f, -1.0f, 0); v1.setRGB(0, 1, 0); v1.setST(0, 0);
        TexturedVertex v2 = new TexturedVertex();
        v2.setXYZ(1.0f, -1.0f, 0); v2.setRGB(0, 0, 1); v2.setST(1, 0);
        TexturedVertex v3 = new TexturedVertex();
        v3.setXYZ(1.0f, 1.0f, 0); v3.setRGB(1, 1, 1); v3.setST(1, 1);

        TexturedVertex[] vertices = new TexturedVertex[] {v0, v1, v2, v3};
        // Put each 'Vertex' in one FloatBuffer
        FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(vertices.length *
                TexturedVertex.elementCount);
        for (int i = 0; i < vertices.length; i++) {
            // Add position, color and texture floats to the buffer
            verticesBuffer.put(vertices[i].getElements());
        }
        verticesBuffer.flip();
        // OpenGL expects to draw vertices in counter clockwise order by default
        byte[] indices = {
                0, 1, 2,
                2, 3, 0
        };
        indicesCount = indices.length;
        ByteBuffer indicesBuffer = BufferUtils.createByteBuffer(indicesCount);
        indicesBuffer.put(indices);
        indicesBuffer.flip();

        // Create a new Vertex Array Object in memory and select it (bind)
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        // Create a new Vertex Buffer Object in memory and select it (bind)
        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW);

        // Put the position coordinates in attribute list 0
        GL20.glVertexAttribPointer(0, TexturedVertex.positionElementCount, GL11.GL_FLOAT,
                false, TexturedVertex.stride, TexturedVertex.positionByteOffset);
        // Put the color components in attribute list 1
        GL20.glVertexAttribPointer(1, TexturedVertex.colorElementCount, GL11.GL_FLOAT,
                false, TexturedVertex.stride, TexturedVertex.colorByteOffset);
        // Put the texture coordinates in attribute list 2
        GL20.glVertexAttribPointer(2, TexturedVertex.textureElementCount, GL11.GL_FLOAT,
                false, TexturedVertex.stride, TexturedVertex.textureByteOffset);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        // Deselect (bind to 0) the VAO
        GL30.glBindVertexArray(0);

        // Create a new VBO for the indices and select it (bind) - INDICES
        vboiId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        this.mc.checkGLError("setupQuad&VBO");
    }

    public void setCorrectedMouse(int mouseXNow, int mouseYNow, int displayWidth, int displayHeight)
    {
        int xOffset = mouseXNow - displayWidth;
        if (xOffset > lastMouseMaxOffsetX)
            lastMouseMaxOffsetX = xOffset;

        if (mouseXNow < lastMouseMaxOffsetX)
        {
            lastMouseMaxOffsetX -= (lastMouseMaxOffsetX - mouseXNow);
            if (lastMouseMaxOffsetX <= 0)
            {
                lastMouseMaxOffsetX = 0;
                mouseXNow = 0;
            }
        }
        mouseX = mouseXNow - lastMouseMaxOffsetX;

        int yOffset = mouseYNow - displayHeight;
        if (yOffset > lastMouseMaxOffsetY)
            lastMouseMaxOffsetY = yOffset;

        if (mouseYNow < lastMouseMaxOffsetY)
        {
            lastMouseMaxOffsetY -= (lastMouseMaxOffsetY - mouseYNow);
            if (lastMouseMaxOffsetY <= 0)
            {
                lastMouseMaxOffsetY = 0;
                mouseYNow = 0;
            }
        }

        mouseY = mouseYNow - lastMouseMaxOffsetY;

        //System.out.println("OffsetY: " + lastMouseMaxOffsetY);
    }

    private FBOParams createFBO(boolean useHighPrecisionBuffer, int fboWidth, int fboHeight)
    {
        int nBufferFormat = GL11.GL_RGBA8;

        if (useHighPrecisionBuffer)
        {
            nBufferFormat = GL11.GL_RGBA16;
        }

        FBOParams fboParams = new FBOParams();

        // The framebuffer, which regroups 0, 1, or more textures, and 0 or 1 depth buffer.
        fboParams._frameBufferId = GL30.glGenFramebuffers();
        fboParams._colorTextureId = GL11.glGenTextures();
        fboParams._depthRenderBufferId = GL30.glGenRenderbuffers();

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboParams._frameBufferId);
        mc.checkGLError("FBO bind framebuffer");

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboParams._colorTextureId);
        mc.checkGLError("FBO bind texture");

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        if (this.mc.gameSettings.useMipMaps)
        {
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        }
        else
        {
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, nBufferFormat, fboWidth, fboHeight, 0, GL11.GL_RGBA, GL11.GL_INT, (java.nio.ByteBuffer) null);
        System.out.println("FBO width: " + fboWidth + ", FBO height: " + fboHeight);
        if (this.mc.gameSettings.useMipMaps)
        {
            // Mipmap gen
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        }

        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, fboParams._colorTextureId, 0);

        mc.checkGLError("FBO bind texture framebuffer");

        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, fboParams._depthRenderBufferId);                // bind the depth renderbuffer
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, fboWidth, fboHeight); // get the data space for it
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, fboParams._depthRenderBufferId);
        mc.checkGLError("FBO bind depth framebuffer");


        mc.checkGLError("After FBO setup");

        return fboParams;
    }

    public void drawQuad()
    {
    	// this func just draws a perfectly normal box with some texture coordinates
        GL11.glBegin(GL11.GL_QUADS);
        
        // Front Face
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(-1.0f, -1.0f,  0.0f);  // Bottom Left Of The Texture and Quad
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f( 1.0f, -1.0f,  0.0f);  // Bottom Right Of The Texture and Quad
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f( 1.0f,  1.0f,  0.0f);  // Top Right Of The Texture and Quad
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(-1.0f, 1.0f, 0.0f);  // Top Left Of The Texture and Quad

        GL11.glEnd();
    }

    public void drawQuad2(float displayWidth, float displayHeight, float scale)
    {
        float aspect = displayHeight / displayWidth;

        GL11.glBegin(GL11.GL_QUADS);

        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(-1.0f * scale, -1.0f * aspect * scale,  0.0f);  // Bottom Left Of The Texture and Quad
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f( 1.0f * scale, -1.0f * aspect * scale,  0.0f);  // Bottom Right Of The Texture and Quad
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f( 1.0f * scale, 1.0f * aspect * scale,  0.0f);  // Top Right Of The Texture and Quad
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(-1.0f * scale, 1.0f * aspect * scale, 0.0f);  // Top Left Of The Texture and Quad

        GL11.glEnd();
    }

    public int initOculusShaders(String vertexShaderGLSL, String fragmentShaderGLSL, boolean doAttribs)
    {
        int vertShader = 0, pixelShader = 0;
        int program = 0;

        try {
            vertShader = createShader(vertexShaderGLSL, ARBVertexShader.GL_VERTEX_SHADER_ARB);
            pixelShader = createShader(fragmentShaderGLSL, ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
        }
        catch(Exception exc) {
            exc.printStackTrace();
            return 0;
        }
        finally {
            if(vertShader == 0 || pixelShader == 0)
                return 0;
        }

        program = ARBShaderObjects.glCreateProgramObjectARB();
        if(program == 0)
            return 0;

        /*
        * if the fragment shaders setup sucessfully,
        * attach them to the shader program, link the shader program
        * into the GL context and validate
        */
        ARBShaderObjects.glAttachObjectARB(program, vertShader);
        ARBShaderObjects.glAttachObjectARB(program, pixelShader);

        if (doAttribs)
        {
            // Position information will be attribute 0
            GL20.glBindAttribLocation(program, 0, "in_Position");
            mc.checkGLError("@2");
            // Color information will be attribute 1
            GL20.glBindAttribLocation(program, 1, "in_Color");
            mc.checkGLError("@2a");
            // Texture information will be attribute 2
            GL20.glBindAttribLocation(program, 2, "in_TextureCoord");
            mc.checkGLError("@3");
        }

        ARBShaderObjects.glLinkProgramARB(program);
        mc.checkGLError("Link");

        if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
            System.out.println(getLogInfo(program));
            return 0;
        }

        ARBShaderObjects.glValidateProgramARB(program);
        if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
            System.out.println(getLogInfo(program));
            return 0;
        }

        return program;
    }

    public final String OCULUS_BASIC_VERTEX_SHADER =

        "#version 110\n" +
        "\n" +
        "void main() {\n" +
        "    gl_Position = ftransform(); //Transform the vertex position\n" +
        "    gl_TexCoord[0] = gl_MultiTexCoord0; // Use Texture unit 0\n" +
        "    //glTexCoord is an openGL defined varying array of vec4. Different elements in the array can be used for multi-texturing with\n" +
        "    //different textures, each requiring their own coordinates.\n" +
        "    //gl_MultiTexCoord0 is an openGl defined attribute vec4 containing the texture coordinates for unit 0 (I'll explain units soon) that\n" +
        "    //you give with calls to glTexCoord2f, glTexCoordPointer etc. gl_MultiTexCoord1 contains unit 1, gl_MultiTexCoord2  unit 2 etc.\n" +
        "}\n";

    public final String OCULUS_DISTORTION_FRAGMENT_SHADER_NO_CHROMATIC_ABERRATION_CORRECTION =

        "#version 120\n" +
        "\n" +
        "uniform sampler2D bgl_RenderTexture;\n" +
        "uniform int half_screenWidth;\n" +
        "uniform vec2 LeftLensCenter;\n" +
        "uniform vec2 RightLensCenter;\n" +
        "uniform vec2 LeftScreenCenter;\n" +
        "uniform vec2 RightScreenCenter;\n" +
        "uniform vec2 Scale;\n" +
        "uniform vec2 ScaleIn;\n" +
        "uniform vec4 HmdWarpParam;\n" +
        "uniform vec4 ChromAbParam;\n" +
        "\n" +
        "// Scales input texture coordinates for distortion.\n" +
        "vec2 HmdWarp(vec2 in01, vec2 LensCenter)\n" +
        "{\n" +
        "    vec2 theta = (in01 - LensCenter) * ScaleIn; // Scales to [-1, 1]\n" +
        "    float rSq = theta.x * theta.x + theta.y * theta.y;\n" +
        "    vec2 rvector = theta * (HmdWarpParam.x + HmdWarpParam.y * rSq +\n" +
        "            HmdWarpParam.z * rSq * rSq +\n" +
        "            HmdWarpParam.w * rSq * rSq * rSq);\n" +
        "    return LensCenter + Scale * rvector;\n" +
        "}\n" +
        "\n" +
        "void main()\n" +
        "{\n" +
        "    // The following two variables need to be set per eye\n" +
        "    vec2 LensCenter = gl_FragCoord.x < half_screenWidth ? LeftLensCenter : RightLensCenter;\n" +
        "    vec2 ScreenCenter = gl_FragCoord.x < half_screenWidth ? LeftScreenCenter : RightScreenCenter;\n" +
        "\n" +
        "    vec2 oTexCoord = gl_TexCoord[0].xy;\n" +
        "    //vec2 oTexCoord = (gl_FragCoord.xy + vec2(0.5, 0.5)) / vec2(screenWidth, screenHeight);\n" +
        "\n" +
        "    vec2 tc = HmdWarp(oTexCoord, LensCenter);\n" +
        "    if (any(bvec2(clamp(tc,ScreenCenter-vec2(0.25,0.5), ScreenCenter+vec2(0.25,0.5)) - tc)))\n" +
        "    {\n" +
        "        gl_FragColor = vec4(vec3(0.0), 1.0);\n" +
        "        return;\n" +
        "    }\n" +
        "\n" +
        "    //tc.x = gl_FragCoord.x < half_screenWidth ? (2.0 * tc.x) : (2.0 * (tc.x - 0.5));\n" +
        "    //gl_FragColor = texture2D(bgl_RenderTexture, tc).aaaa * texture2D(bgl_RenderTexture, tc);\n" +
        "    gl_FragColor = texture2D(bgl_RenderTexture, tc);\n" +
        "}\n";

    public final String OCULUS_DISTORTION_FRAGMENT_SHADER_WITH_CHROMATIC_ABERRATION_CORRECTION =

        "#version 120\n" +
        "\n" +
        "uniform sampler2D bgl_RenderTexture;\n" +
        "uniform int half_screenWidth;\n" +
        "uniform vec2 LeftLensCenter;\n" +
        "uniform vec2 RightLensCenter;\n" +
        "uniform vec2 LeftScreenCenter;\n" +
        "uniform vec2 RightScreenCenter;\n" +
        "uniform vec2 Scale;\n" +
        "uniform vec2 ScaleIn;\n" +
        "uniform vec4 HmdWarpParam;\n" +
        "uniform vec4 ChromAbParam;\n" +
        "\n" +
        "void main()\n" +
        "{\n" +
        "    vec2 LensCenter = gl_FragCoord.x < half_screenWidth ? LeftLensCenter : RightLensCenter;\n" +
        "    vec2 ScreenCenter = gl_FragCoord.x < half_screenWidth ? LeftScreenCenter : RightScreenCenter;\n" +
        "\n" +
        "    vec2 theta = (gl_TexCoord[0].xy - LensCenter) * ScaleIn;\n" +
        "    float rSq = theta.x * theta.x + theta.y * theta.y;\n" +
        "    vec2 theta1 = theta * (HmdWarpParam.x + HmdWarpParam.y * rSq + HmdWarpParam.z * rSq * rSq + HmdWarpParam.w * rSq * rSq * rSq);\n" +
        "\n" +
        "    vec2 thetaBlue = theta1 * (ChromAbParam.w * rSq + ChromAbParam.z);\n" +
        "    vec2 tcBlue = thetaBlue * Scale + LensCenter;\n" +
        "\n" +
        "    if (any(bvec2(clamp(tcBlue, ScreenCenter-vec2(0.25,0.5), ScreenCenter+vec2(0.25,0.5)) - tcBlue))) {\n" +
        "        gl_FragColor = vec4(vec3(0.0), 1.0);\n" +
        "        return;\n" +
        "    }\n" +
        "    float blue = texture2D(bgl_RenderTexture, tcBlue).b;\n" +
        "\n" +
        "    vec2 tcGreen = theta1 * Scale + LensCenter;\n" +
        "    float green = texture2D(bgl_RenderTexture, tcGreen).g;\n" +
        "\n" +
        "    vec2 thetaRed = theta1 * (ChromAbParam.y * rSq + ChromAbParam.x);\n" +
        "    vec2 tcRed = thetaRed * Scale + LensCenter;\n" +
        "    float red = texture2D(bgl_RenderTexture, tcRed).r;\n" +
        "\n" +
        "    gl_FragColor = vec4(red, green, blue, 1.0);\n" +
        "}\n";

    public final String OCULUS_BASIC_FRAGMENT_SHADER =

        "#version 120\n" +
        "\n" +
        "uniform sampler2D bgl_RenderTexture;\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 color = texture2D(bgl_RenderTexture, gl_TexCoord[0].st);\n" +
        "    gl_FragColor = color;\n" +
        "}\n";

    public final String LANCZOS_SAMPLER_VERTEX_SHADER =
        "#version 120\n" +
        "\n" +
        " attribute vec4 in_Position;//position;\n" +
        " attribute vec4 in_Color;//position;\n" +
        " attribute vec2 in_TextureCoord;//inputTextureCoordinate;\n" +
        "\n" +
        " uniform float texelWidthOffset;\n" +
        " uniform float texelHeightOffset;\n" +
        "\n" +
        " varying vec2 centerTextureCoordinate;\n" +
        " varying vec2 oneStepLeftTextureCoordinate;\n" +
        " varying vec2 twoStepsLeftTextureCoordinate;\n" +
        " varying vec2 threeStepsLeftTextureCoordinate;\n" +
        " varying vec2 fourStepsLeftTextureCoordinate;\n" +
        " varying vec2 oneStepRightTextureCoordinate;\n" +
        " varying vec2 twoStepsRightTextureCoordinate;\n" +
        " varying vec2 threeStepsRightTextureCoordinate;\n" +
        " varying vec2 fourStepsRightTextureCoordinate;\n" +
        "\n" +
        " void main()\n" +
        " {\n" +
        "     gl_Position = in_Position;//position;\n" +
        "\n" +
        "     vec2 firstOffset = vec2(texelWidthOffset, texelHeightOffset);\n" +
        "     vec2 secondOffset = vec2(2.0 * texelWidthOffset, 2.0 * texelHeightOffset);\n" +
        "     vec2 thirdOffset = vec2(3.0 * texelWidthOffset, 3.0 * texelHeightOffset);\n" +
        "     vec2 fourthOffset = vec2(4.0 * texelWidthOffset, 4.0 * texelHeightOffset);\n" +
        "\n" +
        "     centerTextureCoordinate = in_TextureCoord;//inputTextureCoordinate;\n" +
        "     oneStepLeftTextureCoordinate = in_TextureCoord - firstOffset;\n" +
        "     twoStepsLeftTextureCoordinate = in_TextureCoord - secondOffset;\n" +
        "     threeStepsLeftTextureCoordinate = in_TextureCoord - thirdOffset;\n" +
        "     fourStepsLeftTextureCoordinate = in_TextureCoord - fourthOffset;\n" +
        "     oneStepRightTextureCoordinate = in_TextureCoord + firstOffset;\n" +
        "     twoStepsRightTextureCoordinate = in_TextureCoord + secondOffset;\n" +
        "     threeStepsRightTextureCoordinate = in_TextureCoord + thirdOffset;\n" +
        "     fourStepsRightTextureCoordinate = in_TextureCoord + fourthOffset;\n" +
        " }\n";

    public final String LANCZOS_SAMPLER_FRAGMENT_SHADER =

        "#version 120\n" +
        "\n" +
        " uniform sampler2D inputImageTexture;\n" +
        "\n" +
        " varying vec2 centerTextureCoordinate;\n" +
        " varying vec2 oneStepLeftTextureCoordinate;\n" +
        " varying vec2 twoStepsLeftTextureCoordinate;\n" +
        " varying vec2 threeStepsLeftTextureCoordinate;\n" +
        " varying vec2 fourStepsLeftTextureCoordinate;\n" +
        " varying vec2 oneStepRightTextureCoordinate;\n" +
        " varying vec2 twoStepsRightTextureCoordinate;\n" +
        " varying vec2 threeStepsRightTextureCoordinate;\n" +
        " varying vec2 fourStepsRightTextureCoordinate;\n" +
        "\n" +
        " // sinc(x) * sinc(x/a) = (a * sin(pi * x) * sin(pi * x / a)) / (pi^2 * x^2)\n" +
        " // Assuming a Lanczos constant of 2.0, and scaling values to max out at x = +/- 1.5\n" +
        "\n" +
        " void main()\n" +
        " {\n" +
        "     vec4 fragmentColor = texture2D(inputImageTexture, centerTextureCoordinate) * 0.38026;\n" +
        "\n" +
        "     fragmentColor += texture2D(inputImageTexture, oneStepLeftTextureCoordinate) * 0.27667;\n" +
        "     fragmentColor += texture2D(inputImageTexture, oneStepRightTextureCoordinate) * 0.27667;\n" +
        "\n" +
        "     fragmentColor += texture2D(inputImageTexture, twoStepsLeftTextureCoordinate) * 0.08074;\n" +
        "     fragmentColor += texture2D(inputImageTexture, twoStepsRightTextureCoordinate) * 0.08074;\n" +
        "\n" +
        "     fragmentColor += texture2D(inputImageTexture, threeStepsLeftTextureCoordinate) * -0.02612;\n" +
        "     fragmentColor += texture2D(inputImageTexture, threeStepsRightTextureCoordinate) * -0.02612;\n" +
        "\n" +
        "     fragmentColor += texture2D(inputImageTexture, fourStepsLeftTextureCoordinate) * -0.02143;\n" +
        "     fragmentColor += texture2D(inputImageTexture, fourStepsRightTextureCoordinate) * -0.02143;\n" +
        "\n" +
        "     gl_FragColor = fragmentColor;\n" +
        " }\n";

    public final String LANCZOS_SAMPLER_FRAGMENT_SHADER2 =
        "#version 120\n" +
        "\n" +
        "uniform sampler2D rubyTexture;\n" +
        "uniform vec2 rubyTextureSize;\n" +
        "\n" +
        "const float PI = 3.1415926535897932384626433832795;\n" +
        "\n" +
        "vec3 weight3(float x)\n" +
        "{\n" +
        "    const float radius = 3.0;\n" +
        "    vec3 sample = FIX(PI * vec3(\n" +
        "        x * 2.0 + 0.0 * 2.0 - 3.0,\n" +
        "        x * 2.0 + 1.0 * 2.0 - 3.0,\n" +
        "        x * 2.0 + 2.0 * 2.0 - 3.0));\n" +
        "\n" +
        "    // Lanczos3\n" +
        "    vec3 ret = 2.0 * sin(sample) * sin(sample / radius) / pow(sample, 2.0);\n" +
        "\n" +
        "    // Normalize\n" +
        "    return ret;\n" +
        "}\n" +
        "\n" +
        "vec3 pixel(float xpos, float ypos)\n" +
        "{\n" +
        "    return texture2D(rubyTexture, vec2(xpos, ypos)).rgb;\n" +
        "}\n" +
        "\n" +
        "vec3 line(float ypos, vec3 xpos1, vec3 xpos2, vec3 linetaps1, vec3 linetaps2)\n" +
        "{\n" +
        "    return\n" +
        "        pixel(xpos1.r, ypos) * linetaps1.r +\n" +
        "        pixel(xpos1.g, ypos) * linetaps2.r +\n" +
        "        pixel(xpos1.b, ypos) * linetaps1.g +\n" +
        "        pixel(xpos2.r, ypos) * linetaps2.g +\n" +
        "        pixel(xpos2.g, ypos) * linetaps1.b +\n" +
        "        pixel(xpos2.b, ypos) * linetaps2.b;\n" +
        "}\n" +
        "\n" +
        "void main()\n" +
        "{\n" +
        "    vec2 stepxy = 1.0 / rubyTextureSize.xy;\n" +
        "    vec2 pos = gl_TexCoord[0].xy + stepxy * 0.5;\n" +
        "    vec2 f = fract(pos / stepxy);\n" +
        "\n" +
        "    vec3 linetaps1   = weight3((1.0 - f.x) / 2.0);\n" +
        "    vec3 linetaps2   = weight3((1.0 - f.x) / 2.0 + 0.5);\n" +
        "    vec3 columntaps1 = weight3((1.0 - f.y) / 2.0);\n" +
        "    vec3 columntaps2 = weight3((1.0 - f.y) / 2.0 + 0.5);\n" +
        "\n" +
        "    // make sure all taps added together is exactly 1.0, otherwise some\n" +
        "    // (very small) distortion can occur\n" +
        "    float suml = dot(linetaps1, 1.0) + dot(linetaps2, 1.0);\n" +
        "    float sumc = dot(columntaps1, 1.0) + dot(columntaps2, 1.0);\n" +
        "    linetaps1 /= suml;\n" +
        "    linetaps2 /= suml;\n" +
        "    columntaps1 /= sumc;\n" +
        "    columntaps2 /= sumc;\n" +
        "\n" +
        "    vec2 xystart = (-2.5 - f) * stepxy + pos;\n" +
        "    vec3 xpos1 = vec3(xystart.x, xystart.x + stepxy.x, xystart.x + stepxy.x * 2.0);\n" +
        "    vec3 xpos2 = vec3(xystart.x + stepxy.x * 3.0, xystart.x + stepxy.x * 4.0, xystart.x + stepxy.x * 5.0);\n" +
        "\n" +
        "    gl_FragColor.rgb =\n" +
        "        line(xystart.y                 , xpos1, xpos2, linetaps1, linetaps2) * columntaps1.r +\n" +
        "        line(xystart.y + stepxy.y      , xpos1, xpos2, linetaps1, linetaps2) * columntaps2.r +\n" +
        "        line(xystart.y + stepxy.y * 2.0, xpos1, xpos2, linetaps1, linetaps2) * columntaps1.g +\n" +
        "        line(xystart.y + stepxy.y * 3.0, xpos1, xpos2, linetaps1, linetaps2) * columntaps2.g +\n" +
        "        line(xystart.y + stepxy.y * 4.0, xpos1, xpos2, linetaps1, linetaps2) * columntaps1.b +\n" +
        "        line(xystart.y + stepxy.y * 5.0, xpos1, xpos2, linetaps1, linetaps2) * columntaps2.b;\n" +
        "\n" +
        "    gl_FragColor.a = 1.0;\n" +
        "}\n";

    private int createShader(String shaderGLSL, int shaderType) throws Exception
    {
        int shader = 0;
        try {
            shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);
            if(shader == 0)
                return 0;

            ARBShaderObjects.glShaderSourceARB(shader, shaderGLSL);
            ARBShaderObjects.glCompileShaderARB(shader);

            if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader: " + getLogInfo(shader));

            return shader;
        }
        catch(Exception exc) {
            ARBShaderObjects.glDeleteObjectARB(shader);
            throw exc;
        }
    }

    private static String getLogInfo(int obj) {
        return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
    }
  

    private float getDistortionFitY()
    {
        float fit = 0.0f;

        switch (this.mc.gameSettings.distortionFitPoint)
        {
            case 0:
                fit = 1.0f;
                break;
            case 1:
                fit = 0.8f;
                break;
            case 2:
                fit = 0.6f;
                break;
            case 3:
                fit = 0.4f;
                break;
            case 4:
                fit = 0.2f;
                break;
            case 5:
            default:
                fit = 0.0f;
                break;
            case 6:
                fit = 0.0f;
                break;
            case 7:
                fit = 0.0f;
                break;
            case 8:
                fit = 0.0f;
                break;
            case 9:
                fit = 0.0f;
                break;
            case 10:
                fit = 0.0f;
                break;
            case 11:
                fit = 0.0f;
                break;
            case 12:
                fit = 0.0f;
                break;
            case 13:
                fit = 0.0f;
                break;
            case 14:
                fit = 0.0f;
                break;
        }

        return fit;
    }

    private float getDistortionFitX()
    {
        float fit = -1.0f;

        switch (this.mc.gameSettings.distortionFitPoint)
        {
            case 0:
                fit = -1.0f;
                break;
            case 1:
                fit = -1.0f;
                break;
            case 2:
                fit = -1.0f;
                break;
            case 3:
                fit = -1.0f;
                break;
            case 4:
                fit = -1.0f;
                break;
            case 5:
            default:
                fit = -1.0f;
                break;
            case 6:
                fit = -0.9f;
                break;
            case 7:
                fit = -0.8f;
                break;
            case 8:
                fit = -0.7f;
                break;
            case 9:
                fit = -0.6f;
                break;
            case 10:
                fit = -0.5f;
                break;
            case 11:
                fit = -0.4f;
                break;
            case 12:
                fit = -0.3f;
                break;
            case 13:
                fit = -0.2f;
                break;
            case 14:
                fit = -0.1f;
                break;
        }

        return fit;
    }

    /**
     * Setup orthogonal projection for rendering GUI screen overlays
     */
    public void setupOverlayRendering(int width, int height, float scaleFactor, IOculusRift oculusRift, EyeRenderParams eyeRenderParams, EntityLiving entity, float renderPartialTicks, int renderSceneNumber)
    {
        // TODO: Setup 3D GUI

        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        // Projection
        EyeRenderParams eye2 = oculusRift.getEyeRenderParams(0, 0,
                (int)ceil(this.mc.displayWidth),
                this.mc.displayHeight,
                0.1f,
                200.0f,
                this.mc.gameSettings.fovScaleFactor,
                getDistortionFitX(),
                getDistortionFitY());

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        if (renderSceneNumber == 0)
        {
            // Left eye
            FloatBuffer leftProj = eye2.gl_getLeftProjectionMatrix();
            GL11.glLoadMatrix(leftProj);
            mc.checkGLError("Set left projection");
        }
        else
        {
            // Right eye
            FloatBuffer rightProj = eye2.gl_getRightProjectionMatrix();
            GL11.glLoadMatrix(rightProj);
            mc.checkGLError("Set right projection");
        }

        // Modelview
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        this.prevCamRoll = this.camRoll;
        float roll = this.camRoll;
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * renderPartialTicks;
        float yaw = (entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * renderPartialTicks) - entity.renderYawOffset;

        if (oculusRift.isInitialized())
        {
            // Use direct values
            pitch = entity.rotationPitch;
            yaw = entity.rotationYaw - entity.renderYawOffset;
        }

        if (this.mc.gameSettings.lockHud && this.mc.gameSettings.lastHudYaw == 1000.0f)
        {
            this.mc.gameSettings.lastHudYaw = yaw;
            this.mc.gameSettings.lastHudPitch = pitch;
            this.mc.gameSettings.lastHudRoll = roll;
        }
        else if (this.mc.gameSettings.lockHud)
        {
            yaw = this.mc.gameSettings.lastHudYaw;
            pitch = this.mc.gameSettings.lastHudPitch;
            roll = this.mc.gameSettings.lastHudRoll;
        }

        if (!this.mc.gameSettings.lockHud)
        {
            this.mc.gameSettings.lastHudYaw = 1000.0f;
            this.mc.gameSettings.lastHudPitch = 1000.0f;
            this.mc.gameSettings.lastHudRoll = 1000.0f;
        }

        GL11.glTranslatef(0.0F, 0.0F, -this.mc.gameSettings.hudDistance);

        if (renderSceneNumber == 0)
        {
            // Left eye
            FloatBuffer leftEyeTransform = eye2.gl_getLeftViewportTransform();
            GL11.glMultMatrix(leftEyeTransform);
        }
        else
        {
            // Right eye
            FloatBuffer rightEyeTransform = eye2.gl_getRightViewportTransform();
            GL11.glMultMatrix(rightEyeTransform);
        }

        // Orient relative to view direction
        //GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
    }
}
