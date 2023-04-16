import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@Composable
@Preview
fun app() {
    MaterialTheme {
        val colorBack = 0xffffff
        var linkOfBook by remember { mutableStateOf("") }
        var titleOfBook by remember { mutableStateOf("") }
        var downloadClicked by remember { mutableStateOf(false) }
        var mp3List by remember { mutableStateOf(listOf<String>()) }
        var error by remember { mutableStateOf(false) }
        var downloadProgress by remember { mutableStateOf(mapOf<String, Float>()) }
        var isDownloading by remember { mutableStateOf(false) }
        val downloadJob = remember { mutableStateOf<Job?>(null) }

        Box(
            modifier = Modifier.background(Color(color = colorBack)).fillMaxHeight().fillMaxWidth().clip(RoundedCornerShape(50.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                OutlinedTextField(
                    value = linkOfBook,
                    onValueChange = { linkOfBook = it },
                    label = { Text(text = "Link of the Book to download") },
                    placeholder = {},
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = titleOfBook,
                        onValueChange = { titleOfBook = it },
                        label = { Text(text = "Folder Name") },
                        placeholder = {},
                        modifier = Modifier.padding(16.dp).width(450.dp)
                    )
                    ListFilesButton(
                        onClick = {
                            if (isValidUrl(linkOfBook)) {
                                downloadClicked = true
                                error = false
                            } else {
                                error = true
                            }
                        },
                        modifier = Modifier.padding(top = 24.dp, end = (16.dp)).height(56.dp)
                    )
                }
                if (error) {
                    Text(
                        text = "Please enter a valid URL",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                } else if (downloadClicked) {
                    GlobalScope.launch(Dispatchers.IO) {
                        mp3List = getMp3FromWebPage(linkOfBook)
                    }
                    LazyColumn(
                        modifier = Modifier.padding(16.dp)
                            .height(300.dp)
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        items(mp3List) { file ->
                            FileToDownload(file, downloadProgress[file] ?: 0f)
                        }
                    }
                }
            }
            DownloadButton(
                onClick = {
                    if (!isDownloading) {
                        isDownloading = true
                        downloadJob.value = GlobalScope.launch(Dispatchers.IO) {
                            downloadMp3Files(this, mp3List, titleOfBook) { url, progress ->
                                downloadProgress = downloadProgress.toMutableMap().apply {
                                    this[url] = progress
                                }
                            }
                        }
                    } else {
                        downloadJob.value?.cancel()
                        isDownloading = false
                    }
                },
                isDownloading = isDownloading,
                modifier = Modifier.padding(bottom = 20.dp).align(Alignment.BottomCenter)

            )
        }
    }
}

@Composable
fun DownloadButton(
    onClick: () -> Unit,
    isDownloading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(if (isDownloading) "Cancel Download" else "Download Book")
    }
}


@Composable
fun FileToDownload(
    file: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically // Add this line to center the elements vertically
    ) {
        Text(getSecondToLastElement(file) + "_" + file.substringAfterLast("/"))
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.weight(1f).height(12.dp)
        )
    }
}

fun getSecondToLastElement(url: String): String {
    val urlArray = url.split("/")
    val secondToLastElement = urlArray.getOrNull(urlArray.size - 2) ?: ""
    return secondToLastElement.replace("%20", "_").lowercase().capitalize()
}

@Composable
fun ListFilesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text("List the files to Download")
    }
}

fun isValidUrl(url: String): Boolean {
    return try {
        URL(url).toURI()
        true
    } catch (e: Exception) {
        false
    }
}

fun getMp3FromWebPage(linkOfBook: String): List<String> {
    val mp3List = mutableListOf<String>()
    try {
        val doc: Document = Jsoup.connect(linkOfBook).get()
        val links = doc.select("a[href]")
        for (link in links) {
            val linkHref = link.attr("href")
            if (linkHref.endsWith(".mp3")) {
                mp3List.add(linkHref)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return mp3List
}

fun downloadMp3Files(
    scope: CoroutineScope,
    mp3List: List<String>,
    titleOfBook: String,
    onProgress: (String, Float) -> Unit
) {
    val folder = File(titleOfBook)
    if (!folder.exists()) {
        folder.mkdir()
    }
    for (mp3 in mp3List) {
        val url = URL(mp3)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect()
        val input = connection.inputStream
        val output = FileOutputStream("$titleOfBook/${getSecondToLastElement(mp3)}_${mp3.substringAfterLast("/")}")
        val data = ByteArray(4096)
        var total: Long = 0
        var count: Int
        val fileSize = connection.contentLengthLong
        while (input.read(data).also { count = it } != -1) {
            if (!scope.isActive) {
                break
            }
            total += count.toLong()
            output.write(data, 0, count)
            onProgress(mp3, total.toFloat() / fileSize.toFloat())
        }
        output.close()
        input.close()
        if (!scope.isActive) {
            break
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Title",
        icon = painterResource("icon32.png"),
        state = WindowState(width = 800.dp, height = 700.dp),
    ) {
        app()
    }
}