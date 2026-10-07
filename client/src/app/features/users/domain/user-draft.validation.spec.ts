import { isValid, validateUserDraft } from './user-draft.validation';

describe('validateUserDraft', () => {
  it('accepts a complete draft', () => {
    const problems = validateUserDraft({ username: 'ada', name: 'Ada', email: 'ada@example.com' });

    expect(isValid(problems)).toBe(true);
  });

  it('treats whitespace-only fields as missing and flags a malformed email', () => {
    const problems = validateUserDraft({ username: '  ', name: ' ', email: 'ada@' });

    expect(Object.keys(problems)).toEqual(['username', 'name', 'email']);
  });
});
