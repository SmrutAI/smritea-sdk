
# MemoryCreateMemoryResponse


## Properties

Name | Type
------------ | -------------
`explainTrace` | [ExplainTrace](ExplainTrace.md)
`explicitSkip` | boolean
`factsExtracted` | number
`memories` | [Array&lt;MemoryMemoryResponse&gt;](MemoryMemoryResponse.md)
`skippedCount` | number
`updatedCount` | number

## Example

```typescript
import type { MemoryCreateMemoryResponse } from ''

// TODO: Update the object below with actual values
const example = {
  "explainTrace": null,
  "explicitSkip": null,
  "factsExtracted": null,
  "memories": null,
  "skippedCount": null,
  "updatedCount": null,
} satisfies MemoryCreateMemoryResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MemoryCreateMemoryResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


