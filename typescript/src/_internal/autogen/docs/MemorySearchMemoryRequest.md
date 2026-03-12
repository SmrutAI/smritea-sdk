
# MemorySearchMemoryRequest


## Properties

Name | Type
------------ | -------------
`actorId` | string
`actorType` | string
`appId` | string
`conversationId` | string
`fromTime` | string
`graphDepth` | number
`limit` | number
`method` | [ModelEnumsSearchMethod](ModelEnumsSearchMethod.md)
`query` | string
`threshold` | number
`toTime` | string
`validAt` | string

## Example

```typescript
import type { MemorySearchMemoryRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "actorId": null,
  "actorType": null,
  "appId": null,
  "conversationId": null,
  "fromTime": null,
  "graphDepth": null,
  "limit": null,
  "method": null,
  "query": null,
  "threshold": null,
  "toTime": null,
  "validAt": null,
} satisfies MemorySearchMemoryRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MemorySearchMemoryRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


