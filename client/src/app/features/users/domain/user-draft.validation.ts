import type { UserDraft } from './user.model';

export type UserDraftProblems = Partial<Record<keyof UserDraft, string>>;

const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export const validateUserDraft = (draft: UserDraft): UserDraftProblems => ({
  ...(draft.username.trim() ? {} : { username: 'Username is required' }),
  ...(draft.name.trim() ? {} : { name: 'Name is required' }),
  ...(EMAIL.test(draft.email.trim()) ? {} : { email: 'Enter a valid email address' }),
});

export const isValid = (problems: UserDraftProblems): boolean => Object.keys(problems).length === 0;
