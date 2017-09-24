package com.example.examplemod;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Mod(modid = ExampleMod.MODID,
        name = ExampleMod.MOD_NAME, version = ExampleMod.VERSION)
@SideOnly(Side.CLIENT)
public class ExampleMod
{
    public static final String MODID = "examplemod";
    public static final String MOD_NAME = "Iqabc";
    public static final String VERSION = "1.0";
    public static final KeyBinding key_S = new KeyBinding(Keyboard.getKeyName(Keyboard.KEY_S),Keyboard.KEY_S, "category");
    public static final KeyBinding key_W = new KeyBinding(Keyboard.getKeyName(Keyboard.KEY_W),Keyboard.KEY_W, "category");
    public static final KeyBinding key_A = new KeyBinding(Keyboard.getKeyName(Keyboard.KEY_A),Keyboard.KEY_A, "category");
    public static final KeyBinding key_D = new KeyBinding(Keyboard.getKeyName(Keyboard.KEY_D),Keyboard.KEY_D, "category");
    public static final KeyBinding key_SPACE = new KeyBinding(Keyboard.getKeyName(Keyboard.KEY_SPACE),Keyboard.KEY_SPACE, "category");

    //MyMods!
    private boolean collided = false;
    private boolean rightClick = false;
    private boolean leftClick  = false;
    private boolean fly = false;
    private boolean spAttack = false;
    private int upCount = 0;
    private int spMoveCount = 0;
    private final int period= 50;
    private static final double keyForce = 1.5d;
    private static final double wireForce = 3d;
    private double vx=0,vy=0,vz=0;//velocity
    private Vec3d des =  Vec3d.ZERO;
    private Vec3d acc = new Vec3d(0.1,0.1,0.1);//acceleration
    private Vec3d origin = new Vec3d(0.1,0.1,0.1);//original acceleration
    private Timer timer = null;
    private EntityPlayer mPlayer;

    @Mod.Instance(MODID)
    public static ExampleMod INSTANCE;

    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event) {
    }
    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        //----------keyEvent----------//
        if (event.getSide() == Side.CLIENT)
        {
            ClientRegistry.registerKeyBinding(key_S);
            ClientRegistry.registerKeyBinding(key_W);
            ClientRegistry.registerKeyBinding(key_A);
            ClientRegistry.registerKeyBinding(key_D);
            ClientRegistry.registerKeyBinding(key_SPACE);
        }
        FMLCommonHandler.instance().bus().register(this);
        //----------keyEvent----------//

        timer = new Timer();
        TimerTask task = new TimerTask() {
            @SideOnly(Side.CLIENT)
            @Override
            public void run() {
                //player
                mPlayer = Minecraft.getMinecraft().player;
                if (mPlayer == null) return;
                //左手に鉄の剣を持ってなければreturn
                if (!mPlayer.getHeldItem(EnumHand.OFF_HAND).getItem().getUnlocalizedName().equals("item.swordIron"))return;
                //playerが空中にいなければreturn
                if (!mPlayer.isAirBorne) {
                    vx = vy = vz = 0;
                    acc = Vec3d.ZERO;
                    return;
                }
                //leftClick down
                if(Mouse.isButtonDown(0)&&!leftClick) {
                    leftClick = true;
                }
                //leftClick up
                else if (!Mouse.isButtonDown(0)&&leftClick) {
                    leftClick = false;
                }
                //rightClick down
                if(Mouse.isButtonDown(1)&&!rightClick){
                    rightClick = true;
                    //目線が当たったところのxyz (目的地の座標)
                    des = mPlayer.rayTrace(42,1.0F).hitVec;
                    // (空中にいる&&右ｸﾘｯｸした)なので,立体起動モードをtrue
                    fly = true;
                    //down時の特殊コマンド (キー入力によって初速度を与える)
                    if (key_S.isKeyDown()&&key_SPACE.isKeyDown()){
                        //上後方
                        vx/=2;vy/=2;vz/=2;
                        vx += Math.sin(Math.toRadians(mPlayer.rotationYaw))*keyForce*0.75;
                        vz += -Math.cos(Math.toRadians(mPlayer.rotationYaw))*keyForce*0.75;
                        vy += keyForce*0.5;
                        spMoveCount = 2;
                        spAttack = true;
                    }
                    else if (key_W.isKeyDown()){
                        //前方
                        vx/=2;vy/=2;vz/=2;
                        vx += -Math.sin(Math.toRadians(mPlayer.rotationYaw))*keyForce*0.5;
                        vz += Math.cos(Math.toRadians(mPlayer.rotationYaw))*keyForce*0.5;
                        vy += -Math.sin(Math.toRadians(mPlayer.rotationPitch))*keyForce*0.4;
                        spMoveCount = 5;
                        spAttack = true;
                    }
                    else if (key_A.isKeyDown()){
                        //左
                        vx/=2;vy/=2;vz/=2;
                        vx += Math.cos(Math.toRadians(mPlayer.rotationYaw))*keyForce*0.75;
                        vz += Math.sin(Math.toRadians(mPlayer.rotationYaw))*keyForce*0.75;
                        vy += 0.2;
                        spMoveCount = 4;
                        spAttack = true;
                    }
                    else if (key_D.isKeyDown()){
                        //右
                        vx/=2;vy/=2;vz/=2;
                        vx += -Math.cos(Math.toRadians(mPlayer.rotationYaw))*keyForce*0.75;
                        vz += -Math.sin(Math.toRadians(mPlayer.rotationYaw))*keyForce*0.75;
                        vy += 0.2;
                        spMoveCount = 4;
                        spAttack = true;
                    }
                }
                //rightClick up
                else if (!Mouse.isButtonDown(1)&&rightClick) {
                    rightClick = false;
                }
                //playerが立体起動モードじゃなければreturn
                if (!fly){
                    vx = vy = vz = 0;
                    acc = Vec3d.ZERO;
                    spAttack = false;
                    return;
                }
                //playerがblockに衝突したときtrue
                collided = mPlayer.isCollided;
                if (collided) {
                    //跳ね返り
                    if (key_W.isKeyDown() && mPlayer.rayTrace(6,1.0f).typeOfHit == RayTraceResult.Type.MISS && mPlayer.isCollidedHorizontally) {
                        upCount = 4;
                        double sum = Math.abs(vx) + Math.abs(vz);
                        vx = -Math.sin(Math.toRadians(mPlayer.rotationYaw)) * sum;
                        vz = Math.cos(Math.toRadians(mPlayer.rotationYaw)) * sum;
                    }
                    else {
                        mPlayer.fall(-1, 1);
                        vx = vy = vz = 0;
                        acc = Vec3d.ZERO;
                        fly = false;
                        spAttack = false;
                        return;
                    }
                }

                //目的地との差分
                double difX = des.x - mPlayer.posX;
                double difY = des.y - mPlayer.posY;
                double difZ = des.z - mPlayer.posZ;
                double sum = Math.abs(difX) + Math.abs(difY) + Math.abs(difZ);
                //ワイヤーが引っ張る力 (加速度 m/s^2)
                origin = new Vec3d(wireForce * (difX / sum), wireForce * (difY / sum), wireForce * (difZ / sum));

                //右ｸﾘｯｸされている(ワイヤーを巻き取っている)場合,加速度と速度を更新
                if(rightClick){
                    upCount = 0;
                    //自分で速度を計算するので,一度 0,0,0 にする
                    mPlayer.setVelocity(0, 0, 0);
                    //目的地に近ければ速度を遅くする
                    if (Math.abs(mPlayer.posX - des.x) <= 4) {
                        vx *= 0.9;
                    }
                    if (Math.abs(mPlayer.posY - des.y) <= 4) {
                        vy *= 0.9;
                    }
                    if (Math.abs(mPlayer.posZ - des.z) <= 4) {
                        vz *= 0.9;
                    }
                    //キーボードによる操作が与える加速度と,元のワイヤーの加速度を計算
                    acc = new Vec3d(
                            key_A.isKeyDown() && !key_D.isKeyDown() ? origin.x + Math.cos(Math.toRadians(mPlayer.rotationYaw)) * keyForce : !key_A.isKeyDown() && key_D.isKeyDown() ? origin.x - Math.cos(Math.toRadians(mPlayer.rotationYaw)) * keyForce : origin.x,
                            key_SPACE.isKeyDown()&&!key_S.isKeyDown() ? origin.y + keyForce*0.55 : origin.y,
                            key_A.isKeyDown() && !key_D.isKeyDown() ? origin.z + Math.sin(Math.toRadians(mPlayer.rotationYaw)) * keyForce : !key_A.isKeyDown() && key_D.isKeyDown() ? origin.z - Math.sin(Math.toRadians(mPlayer.rotationYaw)) * keyForce : origin.z
                    );
                }
                //右ｸﾘｯｸされていない場合,加速度を重力のみにし,速度をゆるやかに下げる (その前に操作性向上のため一度上昇する)
                else {
                    upCount++;
                    vx *= 0.96;
                    vz *= 0.96;
                    spAttack = false;
                    if (upCount >= 10) {
                        mPlayer.fall(-1, 0);
                        vx = vy = vz = 0;
                        acc = Vec3d.ZERO;
                        fly = false;
                        spAttack = false;
                        return;
                    }
                    else if (upCount >= 5) {
                        origin = acc = new Vec3d(0, -2.4, 0);
                    }
                    //SPACE キーが押されていれば上昇する
                    else if(key_SPACE.isKeyDown() && !key_S.isKeyDown()) {
                        origin = acc = new Vec3d(0, 3, 0);
                    }
                }
                // 実行周期,インターバル / 1000ミリ秒  (intervalの1秒のうちの割合 [加速度が,1秒にどれほど進むか,の単位なので])
                double ms = period / 1000d;
                //速度 に (加速度 * interval/1second) を加算
                vx += acc.x * ms;
                vy += acc.y * ms;
                vz += acc.z * ms;
                //特殊な動きをした場合(初速度がある)すこし速度を下げる
                if(spMoveCount > 0){
                    vx *= 0.95;
                    vy *= 0.95;
                    vz *= 0.95;
                    spMoveCount--;
                }
                //set
                mPlayer.setVelocity(vx, vy, vz);
                mPlayer.setPosition(mPlayer.posX + vx * ms, mPlayer.posY + vy * ms, mPlayer.posZ + vz * ms);
            }
        };
        timer.schedule(task,5000,period);
    }
    @Mod.EventHandler
    public void postinit(FMLPostInitializationEvent event) {
    }

    //キーボードイベント
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void inputKey(InputEvent.KeyInputEvent e) {
    }

    //落下イベントのメソッド
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void LivingFallEvent(LivingFallEvent e )
    {
        //エンティティ
        EntityLivingBase entityLiving = e.getEntityLiving();
        //playerなら
        if(entityLiving.equals(Minecraft.getMinecraft().player)) {
            System.out.println(entityLiving.getHeldItem(EnumHand.OFF_HAND).getItem().getUnlocalizedName());
            //左手か右手に鉄の剣を持っていたら
            if (entityLiving.getHeldItem(EnumHand.OFF_HAND).getItem().getUnlocalizedName().equals("item.swordIron") || entityLiving.getHeldItem(EnumHand.MAIN_HAND).getItem().getUnlocalizedName().equals("item.swordIron")) {
                //落下速度(距離?) = 0
                e.setDistance(0);
                entityLiving.fallDistance = 0;
                return;
            }
        }
    }
    /** EntityLivingBaseがダメージを負った時のイベント。 */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        Entity sourceEntity = event.getSource().getTrueSource();
        if (sourceEntity == null)return;
        if (sourceEntity.equals(Minecraft.getMinecraft().player) && spAttack) {
            vx*=1.35;
            if (vy < 0){vy=1.5;}
            vx*=1.35;
            spAttack = false;
        }
    }


    @GameRegistry.ObjectHolder(MODID)
    public static class Blocks {
      /*
          public static final MySpecialBlock mySpecialBlock = null; // placeholder for special block below
      */
    }
    @GameRegistry.ObjectHolder(MODID)
    public static class Items {
      /*
          public static final ItemBlock mySpecialBlock = null; // itemblock for the block above
          public static final MySpecialItem mySpecialItem = null; // placeholder for special item below
      */
    }
    @Mod.EventBusSubscriber
    public static class ObjectRegistryHandler {
        /**
         * Listen for the register event for creating custom items
         */
        @SubscribeEvent
        public static void addItems(RegistryEvent.Register<Item> event) {
            //event.getRegistry().register(sample.setRegistryName(MOD_ID,"myFood"));
           /*
             event.getRegistry().register(new ItemBlock(Blocks.myBlock).setRegistryName(MOD_ID, "myBlock"));
             event.getRegistry().register(new MySpecialItem().setRegistryName(MOD_ID, "mySpecialItem"));
            */
        }

        /**
         * Listen for the register event for creating custom blocks
         */
        @SubscribeEvent
        public static void addBlocks(RegistryEvent.Register<Block> event) {
           /*
             event.getRegistry().register(new MySpecialBlock().setRegistryName(MOD_ID, "mySpecialBlock"));
            */
        }
    }
}