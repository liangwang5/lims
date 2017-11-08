package com.ponytest.lims.auth.dao.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;

import com.ponytest.lims.auth.dao.IUserDao;
import com.ponytest.lims.auth.model.User;
import com.ponytest.lims.auth.model.type.Group;
import com.ponytest.lims.org.model.Dept;
import com.ponytest.lims.org.model.Site;
import com.ponytest.lims.pub.util.BeanUtil;

@Repository
public class UserDao implements IUserDao {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<User> query(Long siteId, Long deptId, List<Group> group, String value) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);
		Root<User> root = query.from(User.class);
		Join<User, Group> join = root.join("userGroup");
		if (group != null)
			join.on(join.in(group));
		query.select(root);
		if (siteId != null) {
			query.where(builder.equal(root.get("siteUser"), siteId));
		}
		if (deptId != null) {
			query.where(builder.equal(root.get("deptUser"), deptId));
		}
		if (value != null) {
			query.where(builder.or(builder.like(root.get("account"), value), builder.like(root.get("name"), value),
					builder.like(root.get("email"), value), builder.like(root.get("tel"), value)));
		}

		// StringBuffer buf = new StringBuffer();
		// buf.append("select a from ").append(User.class.getName()).append(" a ");
		// if (groupId != null) {
		// buf.append(
		// "inner join (auth_user_group u inner join pub_category c on u.group_id = c.id
		// ) on a.id = u.user_id and c.id = '"
		// + groupId + "' ");
		// }
		// buf.append("where 1 = 1 ");
		// if (siteId != null) {
		// buf.append("and a.siteUser = '" + siteId + "' ");
		// }
		// if (deptId != null) {
		// buf.append("and a.deptUser = '" + deptId + "' ");
		// }
		// if (value != null) {
		// buf.append("and a.account like '%" + value + "%' ");
		// buf.append("or a.name like '%" + value + "%' ");
		// buf.append("or a.email like '%" + value + "%' ");
		// buf.append("or a.tel like '%" + value + "%' ");
		// }
		List<User> list = entityManager.createQuery(query).getResultList();
		if (list.size() > 0) {
			return list;
		} else {
			return null;
		}
	}

	@Override
	public User queryById(long id) {
		return entityManager.find(User.class, id);
	}

	@Override
	public User uniqueCheck(String name) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);
		Root<User> root = query.from(User.class);
		query.select(root).where(builder.equal(root.get("account"), name));
		List<User> list = entityManager.createQuery(query).getResultList();
		if (list.size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}

	@Override
	public User uniqueCheck(long id, String name) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);
		Root<User> root = query.from(User.class);
		query.select(root).where(builder.notEqual(root.get("id"), id), builder.equal(root.get("account"), name));
		List<User> list = entityManager.createQuery(query).getResultList();
		if (list.size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}

	@Override
	@Transactional
	public User add(Long siteId, Long deptId, User user) {
		try {
			if (siteId == null) {
				user.setSiteUser(null);
			} else {
				Site site = entityManager.find(Site.class, siteId);
				user.setSiteUser(site);
			}
			if (deptId == null) {
				user.setDeptUser(null);
			} else {
				Dept dept = entityManager.find(Dept.class, deptId);
				user.setDeptUser(dept);
			}
			String password = user.getPassword();
			String account = user.getAccount();
			if (account == null) {
				return null;
			}
			if (password == null) {
				user.setPassword(new BCryptPasswordEncoder().encode(account));
			} else {
				user.setPassword(new BCryptPasswordEncoder().encode(password));

			}
			entityManager.persist(user);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return user;
	}

	@Override
	@Transactional
	public User update(Long siteId, Long deptId, User user) {
		User oldUser = entityManager.find(User.class, user.getId());
		try {
			BeanUtil.copyJpaEntity(user, oldUser);
			if (siteId == null) {
				oldUser.setSiteUser(null);
			} else {
				Site site = entityManager.find(Site.class, siteId);
				oldUser.setSiteUser(site);
			}
			if (deptId == null) {
				oldUser.setDeptUser(null);
			} else {
				Dept dept = entityManager.find(Dept.class, deptId);
				oldUser.setDeptUser(dept);
			}
			entityManager.persist(oldUser);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return oldUser;
	}

	@Override
	@Transactional
	public List<Long> delete(Long[] ids) {
		ArrayList<Long> list = new ArrayList<Long>();
		try {
			for (long id : ids) {
				User user = queryById(id);
				entityManager.remove(user);
				list.add(id);
			}
			entityManager.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	@Transactional
	public User updateForbid(User user) {
		User oldUser = queryById(user.getId());
		oldUser.setForbid(!oldUser.isForbid());
		oldUser.setForbidReason(user.getForbidReason());
		return oldUser;
	}

	@Override
	@Transactional
	public User updateDisable(long id) {
		User user = queryById(id);
		user.setDisable(!user.isDisable());
		return user;
	}

	@Override
	public List<Group> queryGroup(long groupId) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Group> query = builder.createQuery(Group.class);
		Root<Group> root = query.from(Group.class);
		query.select(root)
				.where(builder.or(builder.equal(root.get("id"), groupId), builder.equal(root.get("parent"), groupId)));
		List<Group> list = entityManager.createQuery(query).getResultList();
		if (list.size() > 0) {
			return list;
		} else {
			return null;
		}
	}

	@Override
	@Transactional
	public Group addContact(long groupId, Long[] ids) {
		Group group = entityManager.find(Group.class, groupId);
		List<User> list = new ArrayList<>();
		for (Long id : ids) {
			User user = entityManager.find(User.class, id);
			list.add(user);
		}
		group.setGroupUser(list);
		entityManager.persist(group);
		return group;
	}

}
