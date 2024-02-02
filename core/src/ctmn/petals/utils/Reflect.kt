package ctmn.petals.utils

import io.github.classgraph.ClassGraph

// not working in android
fun findInheritors(superClass: String, packageName: String): List<Class<*>> {
    val inheritors = mutableListOf<Class<*>>()

    val scanResult = ClassGraph().enableAllInfo().scan()
    try {
        val superClassInfo = scanResult.getClassInfo(superClass)
        if (superClassInfo != null) {
            val subClassInfoList = superClassInfo.subclasses
                .filter { it.packageName.startsWith(packageName) }
            for (subClassInfo in subClassInfoList) {
                inheritors.add(Class.forName(subClassInfo.name))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        scanResult.close()
    }

    return inheritors
}