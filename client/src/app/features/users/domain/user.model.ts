export type UserId = number & { readonly brand: 'UserId' };

export interface User {
  readonly id: UserId;
  readonly username: string;
  readonly name: string;
  readonly email: string;
  readonly active: boolean;
}

export interface UserDraft {
  readonly username: string;
  readonly name: string;
  readonly email: string;
}
