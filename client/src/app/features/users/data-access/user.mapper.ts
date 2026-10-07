import type { User, UserDraft, UserId } from '../domain/user.model';
import type { UserDto, UserRequestDto } from './user.dto';

export const toUser = (dto: UserDto): User => ({
  id: dto.id as UserId,
  username: dto.username,
  name: dto.name,
  email: dto.email,
  active: dto.active,
});

export const toUserRequest = (draft: UserDraft): UserRequestDto => ({
  username: draft.username.trim(),
  name: draft.name.trim(),
  email: draft.email.trim(),
});
