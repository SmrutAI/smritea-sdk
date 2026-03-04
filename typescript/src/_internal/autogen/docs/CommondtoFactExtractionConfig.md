
# CommondtoFactExtractionConfig


## Properties

Name | Type
------------ | -------------
`maxPasses` | number
`maxTokens` | number
`minImportance` | number
`model` | string
`strategy` | string
`temperature` | number

## Example

```typescript
import type { CommondtoFactExtractionConfig } from ''

// TODO: Update the object below with actual values
const example = {
  "maxPasses": null,
  "maxTokens": null,
  "minImportance": null,
  "model": null,
  "strategy": null,
  "temperature": null,
} satisfies CommondtoFactExtractionConfig

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CommondtoFactExtractionConfig
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


