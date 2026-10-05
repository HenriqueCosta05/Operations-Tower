export interface AuthenticatedUser {
  readonly subject: string;
  readonly name: string;
  readonly email: string;
  readonly groups: readonly string[];
}
