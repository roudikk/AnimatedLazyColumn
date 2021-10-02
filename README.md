# Animated LazyColumn
 POC of how you can animate LazyColumn insertions/deletions/moving
 
 DEMO:

https://user-images.githubusercontent.com/22520376/135706701-1dc1ba4d-3822-476e-b10b-5e2060a00678.mp4

Example usage:

```koltin
data class MainItem(
    val id: String,
    val text: String
)

val items = List(10) { MainItem(UUID.randomUUID().toString(), UUID.randomUUID().toString()) }
val state = rememberLazyListState()

AnimatedLazyColumn(
   state = state,
   items = items.map {
       KeyItem(key = it.id, value = it.text) {
           TextItem(viewModel, it)
       }
   }
)
```
