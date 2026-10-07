import { TestBed } from '@angular/core/testing';
import { UserRepository } from '../data-access/user.repository';
import type { User, UserId } from '../domain/user.model';
import { InMemoryUserRepository } from '../testing/in-memory-user.repository';
import { UserFacade } from './user.facade';

const ada: User = {
  id: 1 as UserId,
  username: 'ada',
  name: 'Ada Lovelace',
  email: 'ada@example.com',
  active: true,
};

const setup = (users: readonly User[] = [ada]) => {
  const repository = new InMemoryUserRepository(users);
  TestBed.configureTestingModule({
    providers: [UserFacade, { provide: UserRepository, useValue: repository }],
  });
  return { repository, facade: TestBed.inject(UserFacade) };
};

describe('UserFacade', () => {
  it('shows the users once loaded', () => {
    const { facade } = setup();

    facade.load();

    expect(facade.users()).toEqual([ada]);
    expect(facade.loading()).toBe(false);
  });

  it('reports a load failure without losing the screen state', () => {
    const { facade, repository } = setup();
    repository.failing = true;

    facade.load();

    expect(facade.error()).toBe('Could not load users');
    expect(facade.loading()).toBe(false);
  });

  it('adds a created user to the list', () => {
    const { facade } = setup([]);

    facade.create({ username: 'grace', name: 'Grace Hopper', email: 'grace@example.com' });

    expect(facade.users().map((user) => user.username)).toEqual(['grace']);
  });

  it('does not call the server for an invalid draft and exposes the problems', () => {
    const { facade } = setup([]);

    facade.create({ username: '', name: 'Nameless', email: 'nope' });

    expect(facade.users()).toEqual([]);
    expect(Object.keys(facade.problems())).toEqual(['username', 'email']);
  });

  it('replaces the edited user in place', () => {
    const { facade } = setup();
    facade.load();

    facade.update(ada.id, { username: 'ada', name: 'Ada King', email: 'ada@example.com' });

    expect(facade.users().map((user) => user.name)).toEqual(['Ada King']);
  });

  it('keeps a deactivated user listed as inactive', () => {
    const { facade } = setup();
    facade.load();

    facade.deactivate(ada.id);

    expect(facade.users().map((user) => user.active)).toEqual([false]);
  });

  it('surfaces a failed deactivation', () => {
    const { facade, repository } = setup();
    facade.load();
    repository.failing = true;

    facade.deactivate(ada.id);

    expect(facade.error()).toBe('Could not deactivate the user');
    expect(facade.users()[0].active).toBe(true);
  });
});
