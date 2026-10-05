import type { AuthenticatedUser } from '../models/authenticated-user';

export const operatorUser: AuthenticatedUser = {
  subject: 'user-1',
  name: 'Ada Operator',
  email: 'ada@example.com',
  groups: ['operators'],
};
