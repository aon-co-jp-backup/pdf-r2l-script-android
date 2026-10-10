package tokyo.aon.pdfr2lscript

/** Rust製コア(libpdfr2l_jni.so)への橋渡し。mode: 0=R2L, 1=L2R(script版では無視)。 */
object Native {
    init {
        System.loadLibrary("pdfr2l_jni")
    }

    @JvmStatic
    external fun convert(input: ByteArray, mode: Int): ByteArray
}
