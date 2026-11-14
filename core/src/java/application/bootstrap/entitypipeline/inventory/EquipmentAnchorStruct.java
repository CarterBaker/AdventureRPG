package application.bootstrap.entitypipeline.inventory;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class EquipmentAnchorStruct extends StructPackage {

    /*
     * Where an equipment slot's item is worn on a character: the bone it
     * rides, and a place in the model's bind pose — a point and a rotation in
     * degrees. A worn anchor stretches the item's shape to fill a box of its
     * size centred on the point. A held anchor keeps the item at its natural
     * size and puts its grip — the near end of its shape, centred across it —
     * on the point. A slot may have several anchors, such as pants covering
     * hips and both legs, and its item is drawn once at each.
     */

    // Slot
    private final EquipmentSlot equipmentSlot;

    // Bone
    private final int boneIndex;

    // Placement
    private final Vector3 position;
    private final Vector3 rotation;
    private final Vector3 size;
    private final boolean held;

    // Constructor \\

    public EquipmentAnchorStruct(
            EquipmentSlot equipmentSlot,
            int boneIndex,
            Vector3 position,
            Vector3 rotation,
            Vector3 size,
            boolean held) {

        // Slot
        this.equipmentSlot = equipmentSlot;

        // Bone
        this.boneIndex = boneIndex;

        // Placement
        this.position = position;
        this.rotation = rotation;
        this.size = size;
        this.held = held;
    }

    // Accessible \\

    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    public int getBoneIndex() {
        return boneIndex;
    }

    public Vector3 getPosition() {
        return position;
    }

    public Vector3 getRotation() {
        return rotation;
    }

    public Vector3 getSize() {
        return size;
    }

    public boolean isHeld() {
        return held;
    }
}
