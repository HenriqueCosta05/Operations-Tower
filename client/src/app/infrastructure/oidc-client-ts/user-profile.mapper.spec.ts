import { toAuthenticatedUser } from './user-profile.mapper';

describe('toAuthenticatedUser', () => {
  it('maps the standard OIDC claims and the authentik groups claim', () => {
    const user = toAuthenticatedUser({
      sub: 'abc',
      name: 'Ada Operator',
      email: 'ada@example.com',
      groups: ['operators', 'admins'],
    });

    expect(user).toEqual({
      subject: 'abc',
      name: 'Ada Operator',
      email: 'ada@example.com',
      groups: ['operators', 'admins'],
    });
  });

  it('falls back to the username, then the subject, when no display name is released', () => {
    expect(toAuthenticatedUser({ sub: 'abc', preferred_username: 'ada' }).name).toBe('ada');
    expect(toAuthenticatedUser({ sub: 'abc' }).name).toBe('abc');
  });

  it('ignores a groups claim that is not a list of strings', () => {
    expect(toAuthenticatedUser({ sub: 'abc', groups: 'operators' }).groups).toEqual([]);
    expect(toAuthenticatedUser({ sub: 'abc', groups: ['ops', 7] }).groups).toEqual(['ops']);
  });
});
