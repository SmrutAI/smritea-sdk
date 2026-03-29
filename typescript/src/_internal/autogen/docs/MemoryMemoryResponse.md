
# MemoryMemoryResponse


## Properties

Name | Type
------------ | -------------
`activeFrom` | string
`activeTo` | string
`appId` | string
`content` | string
`createdAt` | string
`id` | string
`metadata` | object
`relativeStanding` | [CommondtoRelativeStandingConfig](CommondtoRelativeStandingConfig.md)
`scope` | [CommondtoMemoryScope](CommondtoMemoryScope.md)
`updatedAt` | string

## Example

```typescript
import type { MemoryMemoryResponse } from ''

// TODO: Update the object below with actual values
const example = {
  "activeFrom": null,
  "activeTo": null,
  "appId": null,
  "content": null,
  "createdAt": null,
  "id": null,
  "metadata": null,
  "relativeStanding": null,
  "scope": null,
  "updatedAt": null,
} satisfies MemoryMemoryResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MemoryMemoryResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


