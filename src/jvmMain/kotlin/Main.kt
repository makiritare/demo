import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.window.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


/* make app to download mp3 files from webpage */
@Composable
@Preview
fun app() {
    MaterialTheme {
        val colorBack = 0x00FFFF
        var linkOfBook by remember { mutableStateOf("") }
        var titleOfBook by remember { mutableStateOf("") }
        var downloadClicked by remember { mutableStateOf(false) } // new state variable
        var mp3List by remember { mutableStateOf(mutableListOf<String>()) }
        var error by remember { mutableStateOf(false) } // new state variable for error message

        Column (
            modifier = Modifier.background(Color(color = colorBack)).fillMaxHeight().fillMaxWidth()
        ) {
            OutlinedTextField(
                value = linkOfBook,
                onValueChange = { linkOfBook = it},
                label = { Text(text = "Link of the Book to download") },
                placeholder = {},
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            )
            Row( modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
                OutlinedTextField(
                    value = titleOfBook,
                    onValueChange = { titleOfBook = it },
                    label = { Text(text = "Title of the Book") },
                    placeholder = {},
                    modifier = Modifier.padding(16.dp).width(450.dp)
                )
                // add DownloadButton
                ListFilesButton(
                    onClick = {
                        if (isValidUrl(linkOfBook)) {
                            downloadClicked = true
                            error = false
                        } else {
                            error = true
                        }
                    },
                    //modifier of button to align to the right of the screen
                    modifier = Modifier.padding(top = 24.dp, end = (16.dp)).height(56.dp) )
            }
            if (error) {
                Text(
                    text = "Please enter a valid URL",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (downloadClicked) {
                mp3List = getMp3FromWebPage(linkOfBook)
                    LazyColumn(modifier = Modifier.padding(16.dp)
                        .height(300.dp)
                        .fillMaxWidth()
                        .background(Color.Gray)
                    ){
                        items(mp3List) {
                            Text(text = it, fontSize = 12.sp
                            , modifier = Modifier.padding(start = 16.dp))
                        }
                    }
            }
        //add button to download files
        DownloadButton(
            onClick = {
                downloadMp3Files(mp3List, titleOfBook)
            },
            //padding to bottom of screen
            modifier = Modifier.padding(bottom = 20.dp)
        )
        }
    }
}

//DownloadButton
@Composable
fun DownloadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(16.dp).fillMaxWidth()
    ) {
        Text("Download Book")
    }
}

//ListFilesButton
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

// function to check if a given string is a valid URL
fun isValidUrl(url: String): Boolean {
    return try {
        URL(url).toURI()
        true
    } catch (e: Exception) {
        false
    }
}

//function to get all the mp3 files from the webpage using jsoup and save them to an array using linkOfBook as the url
fun getMp3FromWebPage(linkOfBook: String): MutableList<String> {
    val mp3List = mutableStateListOf<String>()
    val doc: Document = Jsoup.connect(linkOfBook).get()
    val links = doc.select("a[href]")
    for (link in links) {
        if (link.attr("href").contains(".mp3")) {
            mp3List.add(link.attr("href"))
        }
    }
    return mp3List
}

//function to download the mp3 files from the array and save them to the local machine using titleOfBook as the name of the folder

fun downloadMp3Files(mp3List: MutableList<String>, titleOfBook: String) {
    val folder = File(titleOfBook)
    folder.mkdir()
    for (link in mp3List) {
        val fileName = link.substring(link.lastIndexOf("/") + 1)
        val file = File(folder, fileName)
        try {
            val url = URL(link)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            val input = connection.inputStream
            val output = FileOutputStream(file)
            val data = ByteArray(1024)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                output.write(data, 0, count)
            }
            output.flush()
            output.close()
            input.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

fun main() = application {
    //change size of window
    Window(
        onCloseRequest = ::exitApplication,
        title = "Title",
        icon = painterResource("icon32.png"),
        state = WindowState(width = 800.dp, height = 700.dp),
    ) {
        app()
    }
}
