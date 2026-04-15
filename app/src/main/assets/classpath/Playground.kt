import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.*
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
@Composable
fun HelloCompose() {
    Text("Hello Jetpack Composes!")
        val tools =
        listOf(
            Triple(Icons.Default.Settings, Color(0x8F9FCCBC)) {
              
            },
            
        )
        
           LazyRow(
        horizontalArrangement = Arrangement.spacedBy(50.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
    ) {
      items(tools) { (icon, color, action) ->
        // 工具与服务按钮
        Surface(
            onClick = action,
            modifier = Modifier.size(42.dp),
            color = color,
            shape = RoundedCornerShape(10.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            // 工具与服务内部Icon的尺寸
            Icon(
                icon,
                null,
                tint = Color.DarkGray.copy(alpha = 1.8f),
                modifier = Modifier.size(54.dp),
            )
          }
        }
      }
    }
}

@Preview()
@Composable
fun Main() {
    HelloCompose()
}