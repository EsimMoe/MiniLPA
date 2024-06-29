package moe.sekiu.minilpa

import java.lang.invoke.MethodHandles
import sun.misc.Unsafe

object CatMagic
{
    val usf = with(Unsafe::class.java.getDeclaredField("theUnsafe"))
    {
        isAccessible = true
        get(null).cast<Unsafe>()
    }

    val lookup = with(MethodHandles.Lookup::class.java)
    {
        val offset = usf.staticFieldOffset(getDeclaredField("IMPL_LOOKUP"))
        usf.getObject(this, offset).cast<MethodHandles.Lookup>()
    }
}