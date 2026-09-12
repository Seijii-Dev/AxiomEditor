package auto.axiom.editor.github

import com.google.gson.annotations.SerializedName

data class Repository(
    val id: Long,
    val name: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("default_branch") val defaultBranch: String,
    @SerializedName("private") val isPrivate: Boolean,
    @SerializedName("html_url") val htmlUrl: String
)
