import type { AuthenticatedUser } from '../../core/models/authenticated-user';

interface OidcProfile {
  readonly sub: string;
  readonly name?: string;
  readonly email?: string;
  readonly preferred_username?: string;
  readonly groups?: unknown;
}

export const toAuthenticatedUser = (profile: OidcProfile): AuthenticatedUser => ({
  subject: profile.sub,
  name: profile.name ?? profile.preferred_username ?? profile.sub,
  email: profile.email ?? '',
  groups: Array.isArray(profile.groups)
    ? profile.groups.filter((group): group is string => typeof group === 'string')
    : [],
});
