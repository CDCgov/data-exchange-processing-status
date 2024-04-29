export interface User {
    userId: string
    userType: string
    organization: Organization[]
    dataStreams: DataStream[]
}

export interface Organization {
    orgId: number
    orgName: string
    orgRole: string
}

export interface DataStream {
    id: number
    code: string
    name: string
    route: string
    jurisdictions: string[]
}
