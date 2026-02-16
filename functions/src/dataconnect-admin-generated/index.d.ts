import { ConnectorConfig, DataConnect, OperationOptions, ExecuteOperationResponse } from 'firebase-admin/data-connect';

export const connectorConfig: ConnectorConfig;

export type TimestampString = string;
export type UUIDString = string;
export type Int64String = string;
export type DateString = string;


export interface CreateSubjectData {
  subject_insert: Subject_Key;
}

export interface CreateSubjectVariables {
  name: string;
  description?: string | null;
  userId: UUIDString;
}

export interface GetGoalsForUserData {
  goals: ({
    id: UUIDString;
    description?: string | null;
    targetValue: number;
    targetUnit: string;
    targetDate: DateString;
  } & Goal_Key)[];
}

export interface GetGoalsForUserVariables {
  userId: UUIDString;
}

export interface GetTasksForSubjectData {
  tasks: ({
    id: UUIDString;
    name: string;
    description?: string | null;
    isCompleted?: boolean | null;
  } & Task_Key)[];
}

export interface GetTasksForSubjectVariables {
  subjectId: UUIDString;
}

export interface Goal_Key {
  id: UUIDString;
  __typename?: 'Goal_Key';
}

export interface StudySession_Key {
  id: UUIDString;
  __typename?: 'StudySession_Key';
}

export interface Subject_Key {
  id: UUIDString;
  __typename?: 'Subject_Key';
}

export interface Task_Key {
  id: UUIDString;
  __typename?: 'Task_Key';
}

export interface UpdateTaskCompletionData {
  task_update?: Task_Key | null;
}

export interface UpdateTaskCompletionVariables {
  id: UUIDString;
  isCompleted: boolean;
}

export interface User_Key {
  id: UUIDString;
  __typename?: 'User_Key';
}

/** Generated Node Admin SDK operation action function for the 'CreateSubject' Mutation. Allow users to execute without passing in DataConnect. */
export function createSubject(dc: DataConnect, vars: CreateSubjectVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateSubjectData>>;
/** Generated Node Admin SDK operation action function for the 'CreateSubject' Mutation. Allow users to pass in custom DataConnect instances. */
export function createSubject(vars: CreateSubjectVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateSubjectData>>;

/** Generated Node Admin SDK operation action function for the 'GetTasksForSubject' Query. Allow users to execute without passing in DataConnect. */
export function getTasksForSubject(dc: DataConnect, vars: GetTasksForSubjectVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetTasksForSubjectData>>;
/** Generated Node Admin SDK operation action function for the 'GetTasksForSubject' Query. Allow users to pass in custom DataConnect instances. */
export function getTasksForSubject(vars: GetTasksForSubjectVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetTasksForSubjectData>>;

/** Generated Node Admin SDK operation action function for the 'UpdateTaskCompletion' Mutation. Allow users to execute without passing in DataConnect. */
export function updateTaskCompletion(dc: DataConnect, vars: UpdateTaskCompletionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateTaskCompletionData>>;
/** Generated Node Admin SDK operation action function for the 'UpdateTaskCompletion' Mutation. Allow users to pass in custom DataConnect instances. */
export function updateTaskCompletion(vars: UpdateTaskCompletionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateTaskCompletionData>>;

/** Generated Node Admin SDK operation action function for the 'GetGoalsForUser' Query. Allow users to execute without passing in DataConnect. */
export function getGoalsForUser(dc: DataConnect, vars: GetGoalsForUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetGoalsForUserData>>;
/** Generated Node Admin SDK operation action function for the 'GetGoalsForUser' Query. Allow users to pass in custom DataConnect instances. */
export function getGoalsForUser(vars: GetGoalsForUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetGoalsForUserData>>;

