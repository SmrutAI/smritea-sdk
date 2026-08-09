
# SearchMemoryResult


## Properties

Name | Type
------------ | -------------
`activeFrom` | string
`activeTo` | string
`content` | string
`id` | string
`metadata` | object
`scope` | [MemoryScope](MemoryScope.md)

## Example

```typescript
import type { SearchMemoryResult } from ''

// TODO: Update the object below with actual values
const example = {
  "activeFrom": null,
  "activeTo": null,
  "content": null,
  "id": null,
  "metadata": null,
  "scope": null,
} satisfies SearchMemoryResult

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as SearchMemoryResult
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


