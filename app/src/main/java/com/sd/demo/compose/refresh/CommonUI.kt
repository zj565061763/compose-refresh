package com.sd.demo.compose.refresh

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ColumnView(
  list: List<String>,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize()
  ) {
    items(list) { item ->
      ColumnViewItem(text = item)
    }

    item { Box(modifier = Modifier.height(50.dp)) }
  }
}

@Composable
fun ColumnViewItem(
  text: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(50.dp)
      .background(color = Color(0xFFCCCCCC))
  ) {
    Text(
      text = text,
      modifier = Modifier.align(Alignment.Center),
    )
  }
}


@Composable
fun RowView(
  list: List<String>,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier.fillMaxSize()
  ) {
    items(list) { item ->
      RowViewItem(text = item)
    }

    item { Box(modifier = Modifier.width(50.dp)) }
  }
}

@Composable
fun RowViewItem(
  text: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxHeight()
      .width(50.dp)
      .background(color = Color(0xFFCCCCCC))
  ) {
    Text(
      text = text,
      modifier = Modifier.align(Alignment.Center),
    )
  }
}