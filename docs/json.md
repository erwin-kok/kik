# `omitempty`

In Go we can control whether to serialize a value, using `omitempty`. If `omitempty` is not present, its value is always serialized.

e.g.:

```go
type Student struct {
Name string `json:"name"`
Age  int    `json:"age"`
}
```

Even if a value is empty, (for example `Age`), it is serialized:

```json
{
  "name": "Denny",
  "age": 0
}
```

To prevent empty values to be serialized, `omitempty` is used. In this instance empty
values are not serialized. e.g.:

```go
type Student struct {
Name string `json:"name"`
Age  int    `json:"age,omitempty"`
}
```

Will be serialized to (when `Age` is empty):

```json
{
  "name": "Denny"
}
```

Empty values are:

- `false` for boolean
- `0` for numeric types
- `nil` for pointers, interfaces, maps, slices, and channels
- An empty string for string types

In `Kotlin` we have `nullablility`, a value is empty when it is `nullable`:

`Boolean?`, `Int?`, `Car?`, `String?`, etc.

When a property is not-nullable, it is always serialized. This is similar to not having `omitempty`.
When a property is nullable, it is serialized when not-null.

# `UnknownFields`

When a field is present in json, but there is no corresponding field in the Go struct, the field is omitted unless a variable `UnknownFields` is declared in the Go struct. In that case this field is populated with those unknown fields.

# `-`

In Go, when the json name is `-` it is transient. i.e. it is never serialized.

