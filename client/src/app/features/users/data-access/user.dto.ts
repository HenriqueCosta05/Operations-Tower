export interface UserDto {
  readonly id: number;
  readonly username: string;
  readonly name: string;
  readonly email: string;
  readonly active: boolean;
}

export interface UserRequestDto {
  readonly username: string;
  readonly name: string;
  readonly email: string;
}
