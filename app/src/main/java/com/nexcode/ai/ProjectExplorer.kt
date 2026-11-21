package com.nexcode.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile

private val ExplorerInk = Color(0xFFEAF0FF)
private val ExplorerMuted = Color(0xFF9AA8C4)
private val ExplorerAccent = Color(0xFF72E0C1)
private val ExplorerPanel = Color(0xFF131B2E)

@Composable
internal fun ProjectExplorer(
    root: DocumentFile?,
    currentFolder: DocumentFile?,
    onPickProject: () -> Unit,
    onFolderSelected: (DocumentFile) -> Unit,
    onFileSelected: (DocumentFile) -> Unit,
    onCreateFile: () -> Unit,
    onSaveCurrent: () -> Unit,
    fileName: String,
    code: String
) {
    Column(Modifier.fillMaxWidth().fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PROJECT EXPLORER", color = ExplorerAccent, fontSize = 11.sp, letterSpacing = 1.4.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onPickProject) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Choose project folder", tint = ExplorerInk)
            }
        }
        if (root == null || currentFolder == null) {
            Surface(color = ExplorerPanel, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.FolderOpen, null, tint = ExplorerAccent)
                    Text("Open a project folder", color = ExplorerInk)
                    Text("Choose a folder and grant Vex access. Files stay in their original location.", color = ExplorerMuted, fontSize = 12.sp)
                    Button(onClick = onPickProject) { Text("Open project") }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { currentFolder.parentFile?.let(onFolderSelected) },
                    enabled = currentFolder.uri != root.uri && currentFolder.parentFile != null
                ) { Icon(Icons.Default.ArrowBack, "Parent folder", tint = ExplorerInk) }
                Text(currentFolder.name ?: "Project", color = ExplorerInk, fontSize = 13.sp, maxLines = 1)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onCreateFile) { Icon(Icons.Default.CreateNewFolder, "Create file", tint = ExplorerAccent) }
            }
            Surface(color = ExplorerPanel, modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
                val entries = currentFolder.listFiles().sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name?.lowercase().orEmpty() })
                if (entries.isEmpty()) {
                    Text("This folder is empty.", color = ExplorerMuted, modifier = Modifier.padding(16.dp), fontSize = 12.sp)
                } else {
                    LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                        items(entries, key = { it.uri.toString() }) { entry ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    if (entry.isDirectory) onFolderSelected(entry) else onFileSelected(entry)
                                }.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (entry.isDirectory) ExplorerAccent else ExplorerMuted
                                )
                                Text(entry.name ?: "Unnamed", color = ExplorerInk, modifier = Modifier.padding(start = 10.dp), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            Surface(color = ExplorerPanel, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("OPEN DOCUMENT", color = ExplorerAccent, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text(fileName, color = ExplorerInk, fontSize = 13.sp)
                    Text("${code.lines().size} lines  •  ${code.length} characters", color = ExplorerMuted, fontSize = 11.sp)
                    OutlinedButton(onClick = onSaveCurrent, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Save, null)
                        Text("Save current file", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
