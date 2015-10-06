package me.ichun.mods.morph.client.model;

import me.ichun.mods.morph.common.handler.PlayerMorphHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import us.ichun.mods.ichunutil.client.model.ModelHelper;
import us.ichun.mods.ichunutil.common.core.EntityHelperBase;
import us.ichun.mods.ichunutil.common.core.util.ObfHelper;
import us.ichun.mods.ichunutil.common.iChunUtil;

import java.nio.FloatBuffer;
import java.util.*;

public class ModelMorph extends ModelBase
{
    public static final long RAND_SEED = "MorphModelRandSeed".hashCode();

    public static final Random RAND = new Random(RAND_SEED);

    public final ModelInfo prevModelInfo;
    public final ModelInfo nextModelInfo;

    public final ArrayList<ModelRenderer> modelList; //Model list to manipulate with progression.

    public final HashMap<ModelRenderer, ModelRenderer> prevCloneToOriMap;
    public final HashMap<ModelRenderer, ModelRenderer> nextCloneToOriMap;

    public final ArrayList<ModelRenderer> prevModels; //Copy of the arraylist of the prev models. Can modify this list but do not modify the objects in the list!
    public final ArrayList<ModelRenderer> nextModels; //Copy of the arraylist of the next models. Can modify this list but do not modify the objects in the list!

    public ModelMorph()
    {
        this(null, null, null, null);
    }

    public ModelMorph(ModelInfo prev, ModelInfo next, Entity oldRef, Entity newRef) //reference ent is for prev model selection.
    {
        prevModelInfo = prev;
        nextModelInfo = next;

        prevCloneToOriMap = new HashMap<ModelRenderer, ModelRenderer>();
        nextCloneToOriMap = new HashMap<ModelRenderer, ModelRenderer>();
        if(prev != null)
        {
            prevModels = ModelHelper.getModelCubesCopy(prev.modelList, this, oldRef);
            int i = -1;
            for(ModelRenderer model : prevModels)
            {
                while(true)
                {
                    i++;
                    if(prev.modelList.get(i).compiled)
                    {
                        prevCloneToOriMap.put(model, prev.modelList.get(i));
                        break;
                    }
                }
            }
        }
        else
        {
            prevModels = new ArrayList<ModelRenderer>();
        }
        if(next != null)
        {
            nextModels = ModelHelper.getModelCubesCopy(next.modelList, this, newRef); //put all the next models in.
            int i = -1;
            for(ModelRenderer model : nextModels)
            {
                while(true)
                {
                    i++;
                    if(next.modelList.get(i).compiled)
                    {
                        nextCloneToOriMap.put(model, next.modelList.get(i));
                        break;
                    }
                }
            }
        }
        else
        {
            nextModels = new ArrayList<ModelRenderer>();
        }

        //Now that we have our reference models, fill up the reference list and create the modelList.
        prepareReferences();
        //By this point, both the prevModel and nextModels should be the same size and number of children and are therefore proper references.

        modelList = ModelHelper.getModelCubesCopy(prevModels, this, null);
    }

    public void prepareReferences()
    {
        int prevAdjust = -1;
        while(prevModels.size() < nextModels.size()) //if the prev reference has less models than the next reference, create empty ones.
        {
            if(prevAdjust == -1)
            {
                prevAdjust = prevModels.size();
            }
            prevModels.add(ModelHelper.buildCopy(nextModels.get(prevModels.size()), this, 0, false, true));
        }
        int nextAdjust = -1;
        while(nextModels.size() < prevModels.size())
        {
            if(nextAdjust == -1)
            {
                nextAdjust = nextModels.size();
            }
            nextModels.add(ModelHelper.buildCopy(prevModels.get(nextModels.size()), this, 0, false, true));
        }

        //If new boxes are created, anchor them to previously created boxes.
        RAND.setSeed(RAND_SEED);
        if(prevAdjust > 0)
        {
            for(int i = prevAdjust; i < prevModels.size(); i++)
            {
                ModelRenderer renderer = prevModels.get(i);
                ModelRenderer anchor = prevModels.get(RAND.nextInt(prevAdjust));
                renderer.setRotationPoint(anchor.rotationPointX, anchor.rotationPointY, anchor.rotationPointZ);
            }
        }
        if(nextAdjust > 0)
        {
            for(int i = nextAdjust; i < nextModels.size(); i++)
            {
                ModelRenderer renderer = nextModels.get(i);
                ModelRenderer anchor = nextModels.get(RAND.nextInt(nextAdjust));
                renderer.setRotationPoint(anchor.rotationPointX, anchor.rotationPointY, anchor.rotationPointZ);
            }
        }
        fillWithChildren(prevModels, nextModels, 0);
    }

    public void render(float renderTick, float progress, Entity prevRef, Entity nextRef)
    {
        GlStateManager.pushMatrix();

        FloatBuffer buffer = GLAllocation.createDirectFloatBuffer(16);
        FloatBuffer buffer1 = GLAllocation.createDirectFloatBuffer(16);

        float scaleX = 1.0F;
        float scaleY = 1.0F;
        float scaleZ = 1.0F;

        if(prevRef != null && nextRef != null)
        {
            GlStateManager.pushMatrix();
            GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
            Render rend = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(prevRef);
            ObfHelper.invokePreRenderCallback(rend, rend.getClass(), prevRef, iChunUtil.proxy.tickHandlerClient.renderTick);
            GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, buffer1);
            GlStateManager.popMatrix();

            float prevScaleX = buffer1.get(0) / buffer.get(0);
            float prevScaleY = buffer1.get(5) / buffer.get(5);
            float prevScaleZ = buffer1.get(8) / buffer.get(8);

            GlStateManager.pushMatrix();
            GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
            rend = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(nextRef);
            ObfHelper.invokePreRenderCallback(rend, rend.getClass(), nextRef, iChunUtil.proxy.tickHandlerClient.renderTick);
            GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, buffer1);
            GlStateManager.popMatrix();

            float nextScaleX = buffer1.get(0) / buffer.get(0);
            float nextScaleY = buffer1.get(5) / buffer.get(5);
            float nextScaleZ = buffer1.get(8) / buffer.get(8);

            scaleX = prevScaleX + (nextScaleX - prevScaleX) * progress;
            scaleY = prevScaleY + (nextScaleY - prevScaleY) * progress;
            scaleZ = prevScaleZ + (nextScaleZ - prevScaleZ) * progress;

            if(prevModelInfo != null)
            {
                prevModelInfo.forceRender(prevRef, 0D, -500D, 0D, EntityHelperBase.interpolateRotation(prevRef.prevRotationYaw, prevRef.rotationYaw, renderTick), renderTick);
                for(Map.Entry<ModelRenderer, ModelRenderer> e : prevCloneToOriMap.entrySet())
                {
                    matchRotation(e.getKey(), e.getValue(), 0);
                }
            }
            if(nextModelInfo != null)
            {
                nextModelInfo.forceRender(nextRef, 0D, -500D, 0D, EntityHelperBase.interpolateRotation(nextRef.prevRotationYaw, nextRef.rotationYaw, renderTick), renderTick);
                for(Map.Entry<ModelRenderer, ModelRenderer> e : nextCloneToOriMap.entrySet())
                {
                    matchRotation(e.getKey(), e.getValue(), 0);
                }
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(PlayerMorphHandler.getInstance().getMorphSkinTexture());
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.scale(scaleX, scaleY, scaleZ);
        GlStateManager.translate(0F, -1.5F, 0F);

        updateModelList(progress, modelList, prevModels, nextModels, 0);

        for(ModelRenderer cube : modelList)
        {
            cube.render(0.0625F);
        }

        GlStateManager.popMatrix();
    }

    public void matchRotation(ModelRenderer clone, ModelRenderer ori, int depth)
    {
        if(depth > 20)
        {
            return;
        }
        clone.setRotationPoint(ori.rotationPointX, ori.rotationPointY, ori.rotationPointZ);
        clone.rotateAngleX = ori.rotateAngleX;
        clone.rotateAngleY = ori.rotateAngleY;
        clone.rotateAngleZ = ori.rotateAngleZ;

        if(ori.childModels != null)
        {
            for(int i = 0; i < ori.childModels.size(); i++)
            {
                ModelRenderer cloneChild = (ModelRenderer)clone.childModels.get(i);
                ModelRenderer child = (ModelRenderer)ori.childModels.get(i);

                matchRotation(cloneChild, child, depth + 1);
            }
        }
    }

    public void updateModelList(float progress, List modelList, List prevModelList, List nextModelList, int depth)
    {
        if(modelList == null || depth > 20)
        {
            return;
        }
        for(int i = 0; i < modelList.size(); i++)
        {
            ModelRenderer renderer = (ModelRenderer)modelList.get(i);
            ModelRenderer prevRend = (ModelRenderer)prevModelList.get(i);
            ModelRenderer nextRend = (ModelRenderer)nextModelList.get(i);

            for(int j = renderer.cubeList.size() - 1; j >= 0; j--)
            {
                ModelBox box = (ModelBox)renderer.cubeList.get(j);
                ModelBox prevBox = (ModelBox)prevRend.cubeList.get(j);
                ModelBox nextBox = (ModelBox)nextRend.cubeList.get(j);

                int x = (int)Math.abs(box.posX2 - box.posX1);
                int y = (int)Math.abs(box.posY2 - box.posY1);
                int z = (int)Math.abs(box.posZ2 - box.posZ1);

                int px = (int)Math.abs(prevBox.posX2 - prevBox.posX1);
                int py = (int)Math.abs(prevBox.posY2 - prevBox.posY1);
                int pz = (int)Math.abs(prevBox.posZ2 - prevBox.posZ1);

                int nx = (int)Math.abs(nextBox.posX2 - nextBox.posX1);
                int ny = (int)Math.abs(nextBox.posY2 - nextBox.posY1);
                int nz = (int)Math.abs(nextBox.posZ2 - nextBox.posZ1);

                int xx = Math.round(px + (nx - px) * progress);
                int yy = Math.round(py + (ny - py) * progress);
                int zz = Math.round(pz + (nz - pz) * progress);

                float offsetX = EntityHelperBase.interpolateValues(prevBox.posX1, nextBox.posX1, progress);
                float offsetY = EntityHelperBase.interpolateValues(prevBox.posY1, nextBox.posY1, progress);
                float offsetZ = EntityHelperBase.interpolateValues(prevBox.posZ1, nextBox.posZ1, progress);

                if(!(x == xx && y == yy && z == zz && offsetX == box.posX1 && offsetY == box.posY1 && offsetZ == box.posZ1))
                {
                    if(renderer.compiled)
                    {
                        GLAllocation.deleteDisplayLists(renderer.displayList);
                        renderer.compiled = false;
                    }
                    renderer.cubeList.remove(j);
                    renderer.cubeList.add(j, new ModelBox(renderer, renderer.textureOffsetX, renderer.textureOffsetY, offsetX, offsetY, offsetZ, xx, yy, zz, 0.0625F));
                }
            }

            renderer.setRotationPoint(EntityHelperBase.interpolateValues(prevRend.rotationPointX, nextRend.rotationPointX, progress), EntityHelperBase.interpolateValues(prevRend.rotationPointY, nextRend.rotationPointY, progress), EntityHelperBase.interpolateValues(prevRend.rotationPointZ, nextRend.rotationPointZ, progress));
            renderer.rotateAngleX = EntityHelperBase.interpolateValues(prevRend.rotateAngleX, nextRend.rotateAngleX, progress);
            renderer.rotateAngleY = EntityHelperBase.interpolateValues(prevRend.rotateAngleY, nextRend.rotateAngleY, progress);
            renderer.rotateAngleZ = EntityHelperBase.interpolateValues(prevRend.rotateAngleZ, nextRend.rotateAngleZ, progress);

            updateModelList(progress, renderer.childModels, prevRend.childModels, nextRend.childModels, depth + 1);
        }
    }

    public void fillWithChildren(List prevModels, List nextModels, int depth) //fills both lists with children from both sides...hopefully.
    {
        if(prevModels == null || nextModels == null || depth > 20)
        {
            return;
        }
        for(int i = 0; i < (prevModels.size() < nextModels.size() ? prevModels.size() : nextModels.size()); i++) //create empty child clones.
        {
            ModelRenderer prevModel = (ModelRenderer)prevModels.get(i);
            ModelRenderer nextModel = (ModelRenderer)nextModels.get(i);

            while(prevModel.cubeList.size() < nextModel.cubeList.size())
            {
                prevModel.addBox(0F, 0F, 0F, 0, 0, 0, 0.0625F);
            }
            while(nextModel.cubeList.size() < prevModel.cubeList.size())
            {
                nextModel.addBox(0F, 0F, 0F, 0, 0, 0, 0.0625F);
            }

            if(nextModel.childModels != null)
            {
                ModelRenderer emptyCopy = ModelHelper.buildCopy(nextModel, this, 0, true, true);
                setRotationPointToZeroWithChildren(emptyCopy.childModels, 0);
                if(prevModel.childModels != null)
                {
                    for(int k = 0; k < prevModel.childModels.size(); k++)
                    {
                        fillWithChildren(((ModelRenderer)prevModel.childModels.get(k)).childModels, ((ModelRenderer)nextModel.childModels.get(k)).childModels, depth + 1);
                    }
                    for(int k = prevModel.childModels.size(); k < emptyCopy.childModels.size(); k++)
                    {
                        prevModel.addChild((ModelRenderer)emptyCopy.childModels.get(k));
                    }
                }
                else
                {
                    prevModel.childModels = emptyCopy.childModels;
                }
            }

            if(prevModel.childModels != null)
            {
                ModelRenderer emptyCopy = ModelHelper.buildCopy(prevModel, this, 0, true, true);
                setRotationPointToZeroWithChildren(emptyCopy.childModels, 0);
                if(nextModel.childModels != null)
                {
                    for(int k = 0; k < nextModel.childModels.size(); k++)
                    {
                        fillWithChildren(((ModelRenderer)nextModel.childModels.get(k)).childModels, ((ModelRenderer)prevModel.childModels.get(k)).childModels, depth + 1);
                    }
                    for(int k = nextModel.childModels.size(); k < emptyCopy.childModels.size(); k++)
                    {
                        nextModel.addChild((ModelRenderer)emptyCopy.childModels.get(k));
                    }
                }
                else
                {
                    nextModel.childModels = emptyCopy.childModels;
                }
            }
        }
    }

    public void setRotationPointToZeroWithChildren(List children, int depth)
    {
        if(children == null || depth > 20)
        {
            return;
        }
        for(int i = 0; i < children.size(); i++)
        {
            ModelRenderer child = (ModelRenderer)children.get(i);
            child.setRotationPoint(0F, 0F, 0F);
            setRotationPointToZeroWithChildren(child.childModels, depth + 1);
        }
    }

    public void clean()
    {
        for(ModelRenderer renderer : modelList)
        {
            if(renderer.compiled)
            {
                GLAllocation.deleteDisplayLists(renderer.displayList);
                renderer.compiled = false;
            }
        }
    }
}
