package org.anddev.andengine.examples;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.Scene;
import org.anddev.andengine.entity.Scene.IOnAreaTouchListener;
import org.anddev.andengine.entity.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.Scene.ITouchArea;
import org.anddev.andengine.entity.handler.runnable.RunnableHandler;
import org.anddev.andengine.entity.primitives.Rectangle;
import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.extension.physics.box2d.Box2DPhysicsSpace;
import org.anddev.andengine.extension.physics.box2d.adt.DynamicPhysicsBody;
import org.anddev.andengine.extension.physics.box2d.adt.PhysicsShape;
import org.anddev.andengine.extension.physics.box2d.adt.StaticPhysicsBody;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.sensor.accelerometer.AccelerometerData;
import org.anddev.andengine.sensor.accelerometer.IAccelerometerListener;

import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.widget.Toast;

/**
 * @author Nicolas Gramlich
 * @since 21:18:08 - 27.06.2010
 */
public class PhysicsJumpExample extends BaseExample implements IAccelerometerListener, IOnSceneTouchListener, IOnAreaTouchListener {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = 360;
	private static final int CAMERA_HEIGHT = 240;

	// ===========================================================
	// Fields
	// ===========================================================

	private Texture mTexture;

	private TiledTextureRegion mBoxFaceTextureRegion;
	private TiledTextureRegion mCircleFaceTextureRegion;

	private Box2DPhysicsSpace mPhysicsSpace;
	private int mFaceCount = 0;
	private RunnableHandler mShootRunnableHandler;
	
	private float mGravityX;
	private float mGravityY;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public Engine onLoadEngine() {
		Toast.makeText(this, "Touch the screen to add objects. Touch an object to shoot it up into the air.", Toast.LENGTH_LONG).show();
		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		return new Engine(new EngineOptions(true, ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera, false));
	}

	@Override
	public void onLoadResources() {
		this.mTexture = new Texture(64, 64, TextureOptions.BILINEAR);
		TextureRegionFactory.setAssetBasePath("gfx/");
		this.mBoxFaceTextureRegion = TextureRegionFactory.createTiledFromAsset(this.mTexture, this, "boxface_tiled.png", 0, 0, 2, 1); // 64x32
		this.mCircleFaceTextureRegion = TextureRegionFactory.createTiledFromAsset(this.mTexture, this, "circleface_tiled.png", 0, 32, 2, 1); // 64x32
		this.mEngine.getTextureManager().loadTexture(this.mTexture);

		this.enableAccelerometerSensor(this);
	}

	@Override
	public Scene onLoadScene() {
		this.mEngine.registerPostFrameHandler(new FPSLogger());

		this.mPhysicsSpace = new Box2DPhysicsSpace();
		this.mPhysicsSpace.createWorld(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		this.mPhysicsSpace.setGravity(0, 2 * SensorManager.GRAVITY_EARTH);

		final Scene scene = new Scene(2);
		scene.setBackgroundColor(0, 0, 0);
		scene.setOnSceneTouchListener(this);

		final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT - 1, CAMERA_WIDTH, 1);
		scene.getBottomLayer().addEntity(ground);
		this.mPhysicsSpace.addStaticBody(new StaticPhysicsBody(ground, 0, 0.5f, 0.5f, PhysicsShape.RECTANGLE));

		final Rectangle roof = new Rectangle(0, 0, CAMERA_WIDTH, 2);
		scene.getBottomLayer().addEntity(roof);
		this.mPhysicsSpace.addStaticBody(new StaticPhysicsBody(roof, 0, 0.5f, 0.5f, PhysicsShape.RECTANGLE));

		final Rectangle left = new Rectangle(0, 0, 1, CAMERA_HEIGHT);
		scene.getBottomLayer().addEntity(left);
		this.mPhysicsSpace.addStaticBody(new StaticPhysicsBody(left, 0, 0.5f, 0.5f, PhysicsShape.RECTANGLE));

		final Rectangle right = new Rectangle(CAMERA_WIDTH - 1, 0, 1, CAMERA_HEIGHT);
		scene.getBottomLayer().addEntity(right);
		this.mPhysicsSpace.addStaticBody(new StaticPhysicsBody(right, 0, 0.5f, 0.5f, PhysicsShape.RECTANGLE));

		scene.registerPreFrameHandler(this.mPhysicsSpace);

		scene.setOnAreaTouchListener(this);

		this.mShootRunnableHandler = new RunnableHandler();
		scene.registerPostFrameHandler(this.mShootRunnableHandler);

		return scene;
	}

	private void addFace(final float pX, final float pY) {
		this.mFaceCount++;

		final AnimatedSprite face;

		if(this.mFaceCount % 2 == 1){
			face = new AnimatedSprite(pX, pY, this.mBoxFaceTextureRegion);
			this.mPhysicsSpace.addDynamicBody(new DynamicPhysicsBody(face, 1, 0.5f, 0.5f, PhysicsShape.RECTANGLE, false));
		} else {
			face = new AnimatedSprite(pX, pY, this.mCircleFaceTextureRegion);
			this.mPhysicsSpace.addDynamicBody(new DynamicPhysicsBody(face, 1, 0.5f, 0.5f, PhysicsShape.CIRCLE, false));
		}

		final Scene scene = this.mEngine.getScene();
		face.animate(new long[]{200,200}, 0, 1, true);
		scene.registerTouchArea(face);
		scene.getTopLayer().addEntity(face);
	}

	@Override
	public boolean onAreaTouched(final ITouchArea pTouchArea, final MotionEvent pSceneMotionEvent) {
		if(pSceneMotionEvent.getAction() == MotionEvent.ACTION_DOWN) {
			this.mShootRunnableHandler.postRunnable(new Runnable() {
				@Override
				public void run() {
					final AnimatedSprite face = (AnimatedSprite)pTouchArea;
					final DynamicPhysicsBody facePhysicsBody = PhysicsJumpExample.this.mPhysicsSpace.findDynamicBodyByShape(face);
					PhysicsJumpExample.this.mPhysicsSpace.setVelocity(facePhysicsBody, PhysicsJumpExample.this.mGravityX * -10, PhysicsJumpExample.this.mGravityY * -10);
				}
			});
		}

		return false;
	}

	public void onLoadComplete() {

	}

	@Override
	public boolean onSceneTouchEvent(final Scene pScene, final MotionEvent pSceneMotionEvent) {
		if(this.mPhysicsSpace != null) {
			if(pSceneMotionEvent.getAction() == MotionEvent.ACTION_DOWN) {
				this.addFace(pSceneMotionEvent.getX(), pSceneMotionEvent.getY());
				return true;
			}
		}
		return false;
	}

	@Override
	public void onAccelerometerChanged(final AccelerometerData pAccelerometerData) {
		this.mGravityX = pAccelerometerData.getY();
		this.mGravityY = pAccelerometerData.getX();
		this.mPhysicsSpace.setGravity(4 * this.mGravityX, 4 * this.mGravityY);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}