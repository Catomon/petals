package ctmn.petals.utils

object Version {

    fun compareVersions(v1: String, v2: String): Int {
        val v1Nums = getNums(v1)
        val v2Nums = getNums(v2)

        for (i in v1Nums.indices) {
            if (v1Nums[i] > v2Nums[i])
                return 1
            else
                if (v1Nums[i] < v2Nums[i])
                    return -1
        }

        return 0
    }

    private fun getNums(ver: String): List<Int> {
        val v1Split = ver.split("-")
        val v1Num = v1Split[0].split(".").map { it.toInt() }
        val v1Str = if (v1Split.size == 2) when (v1Split[1]) {
            "alpha" -> 0; "beta" -> 1; else -> 3
        } else 3
        return v1Num + v1Str
    }
}