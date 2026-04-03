
# MemoryCreateMemoryRequest


## Properties

Name | Type
------------ | -------------
`appId` | string
`content` | string
`entityExtractionOverrides` | [CommondtoEntityExtractionConfig](CommondtoEntityExtractionConfig.md)
`eventOccurredAt` | string
`factExtractionOverrides` | [CommondtoFactExtractionConfig](CommondtoFactExtractionConfig.md)
`metadata` | object
`personaExtractionOverrides` | [CommondtoPersonaExtractionConfig](CommondtoPersonaExtractionConfig.md)
`relativeStanding` | [CommondtoRelativeStandingConfig](CommondtoRelativeStandingConfig.md)
`scope` | [CommondtoMemoryScope](CommondtoMemoryScope.md)

## Example

```typescript
import type { MemoryCreateMemoryRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "appId": null,
  "content": null,
  "entityExtractionOverrides": null,
  "eventOccurredAt": null,
  "factExtractionOverrides": null,
  "metadata": null,
  "personaExtractionOverrides": null,
  "relativeStanding": null,
  "scope": null,
} satisfies MemoryCreateMemoryRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MemoryCreateMemoryRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


