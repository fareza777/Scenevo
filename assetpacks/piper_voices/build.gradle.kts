plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("piper_voices")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
