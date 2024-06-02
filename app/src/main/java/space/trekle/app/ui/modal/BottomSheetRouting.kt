package space.trekle.app.ui.modal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.bottomsheet.material3.FlexibleBottomSheet
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import space.trekle.app.services.routing.RouteResponse
import kotlin.math.roundToInt


@Composable
fun BottomSheetRouting(route: RouteResponse, removeRoute: () -> Unit, cancelPrevious: () -> Unit){
    return FlexibleBottomSheet(
        onDismissRequest = { },
        sheetState = rememberFlexibleBottomSheetState(
            confirmValueChange = { newValue ->
                if (newValue == FlexibleSheetValue.Hidden) {
                    return@rememberFlexibleBottomSheetState false
                }
                true
            },
            flexibleSheetSize = FlexibleSheetSize(
                fullyExpanded = 0.85f,
                intermediatelyExpanded = 0.45f,
                slightlyExpanded = 0.15f,
            ),
            skipSlightlyExpanded = false
        )
    ) {
        MaterialTheme{
            Column {
                Text(
                    text = "Distance " + (route.trip.summary.length * 10).roundToInt() / 10.0f + "km",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Button(
                    onClick = {
                        removeRoute()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Clear route")
                }
                Button(
                    onClick = {
                        cancelPrevious()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Remove last point ")
                }

            }
        }
    }
}