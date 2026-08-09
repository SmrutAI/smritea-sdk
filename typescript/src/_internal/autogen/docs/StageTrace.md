
# StageTrace


## Properties

Name | Type
------------ | -------------
`durationMs` | number
`error` | string
`input` | string
`output` | string
`resultCount` | number
`stageName` | string
`steps` | [Array&lt;StepTrace&gt;](StepTrace.md)

## Example

```typescript
import type { StageTrace } from ''

// TODO: Update the object below with actual values
const example = {
  "durationMs": null,
  "error": null,
  "input": null,
  "output": null,
  "resultCount": null,
  "stageName": null,
  "steps": null,
} satisfies StageTrace

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as StageTrace
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


