/**
 * Copyright (C) 2011  JTalks.org Team
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jtalks.jcommune.service.transactional;

import org.jtalks.common.model.dao.ChildRepository;
import org.jtalks.common.model.dao.GroupDao;
import org.jtalks.common.model.permissions.GeneralPermission;
import org.jtalks.common.security.SecurityService;
import org.jtalks.jcommune.model.entity.Branch;
import org.jtalks.jcommune.model.entity.Poll;
import org.jtalks.jcommune.model.entity.PollItem;
import org.jtalks.jcommune.service.PollService;
import org.jtalks.jcommune.service.security.AdministrationGroup;
import org.jtalks.jcommune.service.security.SecurityConstants;
import org.jtalks.jcommune.service.security.TemporaryAuthorityManager;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * The implementation of the {@link PollService}.
 *
 * @author Anuar Nurmakanov
 * @author Alexandre Teterin
 * @see org.jtalks.jcommune.model.entity.Poll
 */
public class TransactionalPollService extends AbstractTransactionalEntityService<Poll, ChildRepository<Poll>>
        implements PollService {
    private ChildRepository<PollItem> pollOptionDao;
    private GroupDao groupDao;
    private SecurityService securityService;
    private TemporaryAuthorityManager temporaryAuthorityManager;

    /**
     * Create an instance of service for operations with a poll.
     *
     * @param pollDao                   data access object, which should be able do
     *                                  all CRUD operations with {@link Poll}.
     * @param groupDao                  this dao returns user group for permission granting
     * @param pollOptionDao             data access object, which should be able do
     *                                  all CRUD operations with {@link org.jtalks.jcommune.model.entity.PollItem}.
     * @param securityService           the service for security operations
     * @param temporaryAuthorityManager the  manager of temporary authorities, that
     *                                  allows to execute an operation with the
     *                                  needed authority
     */
    public TransactionalPollService(ChildRepository<Poll> pollDao,
                                    GroupDao groupDao,
                                    ChildRepository<PollItem> pollOptionDao,
                                    SecurityService securityService,
                                    TemporaryAuthorityManager temporaryAuthorityManager) {
        super(pollDao);
        this.pollOptionDao = pollOptionDao;
        this.groupDao = groupDao;
        this.securityService = securityService;
        this.temporaryAuthorityManager = temporaryAuthorityManager;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Poll vote(Long pollId, List<Long> pollOptionIds) {
        Poll poll = getDao().get(pollId);
        Branch branch = poll.getTopic().getBranch();
        return this.vote(poll, pollOptionIds, branch.getId());
    }

    /**
     * Performs actual voting with permission check
     * 
     * @param poll poll we're voting in
     * @param selectedOptionIds voting options, selected by user
     * @param branchId used for annotation permission check only
     * @return poll updated with new votes
     */
    @PreAuthorize("hasPermission(#branchId, 'BRANCH', 'BranchPermission.CREATE_POSTS')")
    private Poll vote(Poll poll, List<Long> selectedOptionIds, long branchId) {
        if (poll.isActive()) {
            prohibitRevote(poll);
            for (PollItem option : poll.getPollItems()) {
                if (selectedOptionIds.contains(option.getId())) {
                    option.increaseVotesCount();
                    pollOptionDao.update(option);
                }
            }
        }
        return poll;
    }

    @Override
    public void createPoll(Poll poll) {
        this.getDao().update(poll);
        for (PollItem option : poll.getPollItems()) {
            pollOptionDao.update(option);
        }
        securityService.createAclBuilder().grant(GeneralPermission.WRITE)
                .to(groupDao.get(AdministrationGroup.USER.getId()))
                .on(poll).flush();
    }

    /**
     * Prohibit the re-vote. In this poll a user will no longer be able to participate.
     *
     * @param poll a poll, in which the user will no longer be able to participate
     */
    private void prohibitRevote(final Poll poll) {
        securityService.createAclBuilder().
                restrict(GeneralPermission.WRITE).to(securityService.getCurrentUser()).on(poll).flush();
/*        //TODO It should be changed after the transition to the new security.
        temporaryAuthorityManager.runWithTemporaryAuthority(
                new TemporaryAuthorityManager.SecurityOperation() {
                    @Override
                    public void doOperation() {
                        securityService.createAclBuilder().
                                restrict(GeneralPermission.WRITE).to(securityService.getCurrentUser()).on(poll).flush();
                    }
                },
                SecurityConstants.ROLE_ADMIN);*/
    }
}
