export interface InterviewRecord {
  id: number
  recordName: string
  duration: number
  questionCount: number
  createTime: string
  evaluation?: string
}

export interface InterviewQA {
  id: number
  recordId: number
  question: string
  answer: string
  evaluation: string
  sequence: number
  createTime?: string
}

export interface UpdateRecordNameDTO {
  recordId: number
  name: string
}