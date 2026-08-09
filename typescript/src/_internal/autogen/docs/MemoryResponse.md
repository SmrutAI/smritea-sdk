
# MemoryResponse


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
`relativeStanding` | [RelativeStandingConfig](RelativeStandingConfig.md)
`scope` | [MemoryScope](MemoryScope.md)
`updatedAt` | string

## Example

```typescript
import type { MemoryResponse } from ''

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
} satisfies MemoryResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MemoryResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


