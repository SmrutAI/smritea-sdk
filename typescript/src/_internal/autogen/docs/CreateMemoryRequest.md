
# CreateMemoryRequest


## Properties

Name | Type
------------ | -------------
`appId` | string
`content` | string
`entityExtractionOverrides` | [EntityExtractionConfig](EntityExtractionConfig.md)
`eventOccurredAt` | string
`factExtractionOverrides` | [FactExtractionConfig](FactExtractionConfig.md)
`metadata` | object
`personaExtractionOverrides` | [PersonaExtractionConfig](PersonaExtractionConfig.md)
`relativeStanding` | [RelativeStandingConfig](RelativeStandingConfig.md)
`scope` | [MemoryScope](MemoryScope.md)

## Example

```typescript
import type { CreateMemoryRequest } from ''

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
} satisfies CreateMemoryRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateMemoryRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


