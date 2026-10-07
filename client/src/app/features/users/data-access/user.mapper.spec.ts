import { toUser, toUserRequest } from './user.mapper';

describe('user mapper', () => {
  it('maps the wire format to a user', () => {
    const user = toUser({ id: 7, username: 'ada', name: 'Ada', email: 'a@b.co', active: false });

    expect(user).toEqual({ id: 7, username: 'ada', name: 'Ada', email: 'a@b.co', active: false });
  });

  it('trims the draft before sending it', () => {
    expect(toUserRequest({ username: ' ada ', name: ' Ada ', email: ' a@b.co ' })).toEqual({
      username: 'ada',
      name: 'Ada',
      email: 'a@b.co',
    });
  });
});
