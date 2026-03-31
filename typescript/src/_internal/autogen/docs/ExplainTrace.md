
# ExplainTrace


## Properties

Name | Type
------------ | -------------
`stages` | [Array&lt;ExplainStageTrace&gt;](ExplainStageTrace.md)
`totalMs` | number

## Example

```typescript
import type { ExplainTrace } from ''

// TODO: Update the object below with actual values
const example = {
  "stages": null,
  "totalMs": null,
} satisfies ExplainTrace

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ExplainTrace
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


