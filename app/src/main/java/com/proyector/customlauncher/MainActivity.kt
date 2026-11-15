package com.proyector.customlauncher
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
class MainActivity : AppCompatActivity() {
private lateinit var rootLayout: RelativeLayout
private lateinit var bgImageView: ImageView
private lateinit var timeText: TextView
private lateinit var dateText: TextView
private lateinit var weatherText: TextView
private val handler = Handler(Looper.getMainLooper())
private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
uri?.let {
try {
contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
} catch (_: Exception) {}
getSharedPreferences("prefs", MODE_PRIVATE).edit().putString("bg_uri", it.toString()).apply()
bgImageView.setImageURI(it)
}
}
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
rootLayout = RelativeLayout(this).apply {
background = GradientDrawable(
GradientDrawable.Orientation.TL_BR,
intArrayOf(Color.parseColor("#0F172A"), Color.parseColor("#1E293B"), Color.parseColor("#090D16"))
)
}
bgImageView = ImageView(this).apply {
scaleType = ImageView.ScaleType.CENTER_CROP
layoutParams = RelativeLayout.LayoutParams(-1, -1)
}
getSharedPreferences("prefs", MODE_PRIVATE).getString("bg_uri", null)?.let {
try { bgImageView.setImageURI(Uri.parse(it)) } catch (_: Exception) {}
}
rootLayout.addView(bgImageView)
val topBar = LinearLayout(this).apply {
orientation = LinearLayout.HORIZONTAL
gravity = Gravity.CENTER_VERTICAL
setPadding(36, 20, 36, 20)
background = GradientDrawable().apply {
setColor(Color.parseColor("#77111827"))
cornerRadius = 32f
setStroke(2, Color.parseColor("#33FFFFFF"))
}
layoutParams = RelativeLayout.LayoutParams(-1, -2).apply {
setMargins(48, 36, 48, 0)
}
}
timeText = TextView(this).apply {
textSize = 34f
setTextColor(Color.WHITE)
typeface = Typeface.DEFAULT_BOLD
}
dateText = TextView(this).apply {
textSize = 14f
setTextColor(Color.parseColor("#CBD5E1"))
setPadding(24, 0, 32, 0)
}
weatherText = TextView(this).apply {
text = "Cargando clima..."
textSize = 15f
setTextColor(Color.parseColor("#E2E8F0"))
layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
}
val btnWallpaper = createTopButton("Fondo") { pickImage.launch("image/*") }
val btnSettings = createTopButton("Ajustes") {
startActivity(Intent(Settings.ACTION_SETTINGS))
}
topBar.addView(timeText)
topBar.addView(dateText)
topBar.addView(weatherText)
topBar.addView(btnWallpaper)
topBar.addView(btnSettings)
rootLayout.addView(topBar)
val scrollDock = HorizontalScrollView(this).apply {
isHorizontalScrollBarEnabled = false
background = GradientDrawable().apply {
setColor(Color.parseColor("#880F172A"))
cornerRadius = 40f
setStroke(2, Color.parseColor("#44FFFFFF"))
}
setPadding(28, 22, 28, 22)
layoutParams = RelativeLayout.LayoutParams(-1, -2).apply {
addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
setMargins(48, 0, 48, 36)
}
}
val appsRow = LinearLayout(this).apply {
orientation = LinearLayout.HORIZONTAL
gravity = Gravity.CENTER_VERTICAL
}
loadInstalledApps(appsRow)
scrollDock.addView(appsRow)
rootLayout.addView(scrollDock)
setContentView(rootLayout)
startClock()
fetchWeather()
}
private fun createTopButton(label: String, onClick: () -> Unit): TextView {
return TextView(this).apply {
text = label
textSize = 14f
setTextColor(Color.WHITE)
isFocusable = true
isFocusableInTouchMode = true
setPadding(24, 12, 24, 12)
val normalBg = GradientDrawable().apply {
setColor(Color.parseColor("#33FFFFFF"))
cornerRadius = 20f
}
val focusBg = GradientDrawable().apply {
setColor(Color.parseColor("#3B82F6"))
cornerRadius = 20f
}
background = normalBg
setOnFocusChangeListener { _, hasFocus -> background = if (hasFocus) focusBg else normalBg }
setOnClickListener { onClick() }
layoutParams = LinearLayout.LayoutParams(-2, -2).apply { setMargins(12, 0, 0, 0) }
}
}
private fun loadInstalledApps(container: LinearLayout) {
val pm = packageManager
val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
val apps = pm.queryIntentActivities(intent, 0)
.filter { it.activityInfo.packageName != packageName }
.sortedBy { it.loadLabel(pm).toString().lowercase() }
for (app in apps) {
val card = LinearLayout(this).apply {
orientation = LinearLayout.VERTICAL
gravity = Gravity.CENTER
isFocusable = true
isFocusableInTouchMode = true
setPadding(20, 16, 20, 16)
val normalBg = GradientDrawable().apply {
setColor(Color.parseColor("#22FFFFFF"))
cornerRadius = 24f
}
val focusBg = GradientDrawable().apply {
setColor(Color.parseColor("#443B82F6"))
cornerRadius = 24f
setStroke(3, Color.WHITE)
}
background = normalBg
setOnFocusChangeListener { _, hasFocus ->
background = if (hasFocus) focusBg else normalBg
scaleX = if (hasFocus) 1.08f else 1.0f
scaleY = if (hasFocus) 1.08f else 1.0f
}
setOnClickListener {
pm.getLaunchIntentForPackage(app.activityInfo.packageName)?.let { startActivity(it) }
}
layoutParams = LinearLayout.LayoutParams(160, 135).apply { setMargins(12, 0, 12, 0) }
}
val icon = ImageView(this).apply {
setImageDrawable(app.loadIcon(pm))
layoutParams = LinearLayout.LayoutParams(68, 68)
}
val title = TextView(this).apply {
text = app.loadLabel(pm)
textSize = 12f
maxLines = 1
gravity = Gravity.CENTER
setTextColor(Color.WHITE)
setPadding(0, 10, 0, 0)
}
card.addView(icon)
card.addView(title)
container.addView(card)
}
}
private fun startClock() {
val update = object : Runnable {
override fun run() {
val now = Date()
timeText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
dateText.text = SimpleDateFormat("EEEE\nd 'de' MMMM", Locale("es", "MX")).format(now).uppercase()
handler.postDelayed(this, 15000)
}
}
handler.post(update)
}
private fun fetchWeather() {
thread {
try {
val url = "https://api.open-meteo.com/v1/forecast?latitude=18.96&longitude=-97.89&current_weather=true"
val json = JSONObject(URL(url).readText()).getJSONObject("current_weather")
val temp = json.getDouble("temperature").toInt()
runOnUiThread { weatherText.text = "${temp}°C • Despejado" }
} catch (_: Exception) {
runOnUiThread { weatherText.text = "Modo Cine" }
}
}
}
@Deprecated("Deprecated in Java")
override fun onBackPressed() {}
}
