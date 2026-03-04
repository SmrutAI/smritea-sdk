
# CommondtoEntityExtractionConfig


## Properties

Name | Type
------------ | -------------
`contextWindow` | number
`enableContext` | boolean
`entityTypes` | Array&lt;string&gt;
`fallbackMessages` | number
`maxPasses` | number
`maxTokens` | number
`minConfidence` | number
`model` | string
`temperature` | number

## Example

```typescript
import type { CommondtoEntityExtractionConfig } from ''

// TODO: Update the object below with actual values
const example = {
  "contextWindow": null,
  "enableContext": null,
  "entityTypes": null,
  "fallbackMessages": null,
  "maxPasses": null,
  "maxTokens": null,
  "minConfidence": null,
  "model": null,
  "temperature": null,
} satisfies CommondtoEntityExtractionConfig

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CommondtoEntityExtractionConfig
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


